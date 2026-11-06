#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 生成「我方改动 vs 上游」补丁快照
#
#   bash tools/make-patches.sh [上游基线]
#
# 不传基线时默认取 origin/master 与 HEAD 的 merge-base。
#
# 前置约定：our-changes 分支必须是「线性」的（没有 merge commit），
# 否则拆分出的补丁链无法逐个 git am。见 FORK.md「为什么分支必须线性」。
#
# 产物：
#   patches/tv-fork.diff              单个合并补丁（日常同步用这个，冲突一次解决完）
#   patches/series/NNNN-<sha>.patch   按提交拆分的补丁链（git am / 回看历史用）
#   patches/BASE                      基线 commit 信息
# ---------------------------------------------------------------------------
set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

BASE=${1:-$(git merge-base origin/master HEAD)}
BASE_FULL=$(git rev-parse "$BASE")
BASE_SHA=$(git rev-parse --short "$BASE")
BASE_SUBJECT=$(git log -1 --format=%s "$BASE")
HEAD_SHA=$(git rev-parse --short HEAD)

# patches/ 自身不参与补丁内容，否则补丁会包含自己
FILTER=(-- . ':(exclude)patches')

if [ -n "$(git rev-list --merges "$BASE..HEAD")" ]; then
  echo "警告：$BASE_SHA..HEAD 之间还有 merge commit，补丁链可能无法逐个应用。" >&2
  echo "      执行 git rebase $BASE_SHA 把它变线性后再跑本脚本。" >&2
fi

echo "基线: $BASE_SHA  $BASE_SUBJECT"
echo "tip : $HEAD_SHA  $(git log -1 --format=%s HEAD)"

# patches/ 会整个重建，但里面的说明文档要保留
KEEP_README=""
[ -f patches/README.md ] && KEEP_README=$(cat patches/README.md)

rm -rf patches
mkdir -p patches/series
[ -n "$KEEP_README" ] && printf '%s\n' "$KEEP_README" > patches/README.md

# ---- 1. 单个合并补丁 ------------------------------------------------------
{
  printf '%s\n' \
    "# NeriPlayer TV fork — 相对上游的完整改动（单个合并补丁）" \
    "# 基线 upstream commit : $BASE_SHA  ($BASE_SUBJECT)" \
    "# 我方 tip             : $HEAD_SHA  ($(git log -1 --format=%s HEAD))" \
    "# 生成时间             : $(date -Iseconds)" \
    "# 生成命令             : bash tools/make-patches.sh $BASE_SHA" \
    "# 应用                 : git apply -3 patches/tv-fork.diff" \
    ""
  git diff "$BASE_FULL" HEAD "${FILTER[@]}"
} > patches/tv-fork.diff

# ---- 2. 按提交拆分的补丁链 ------------------------------------------------
mapfile -t COMMITS < <(git rev-list --reverse --no-merges --topo-order "$BASE_FULL..HEAD" "${FILTER[@]}")

i=1
for c in "${COMMITS[@]}"; do
  sha=$(git rev-parse --short "$c")
  git format-patch -1 --no-signature --stdout "$c" > "patches/series/$(printf '%04d' $i)-$sha.patch"
  i=$((i + 1))
done

cat > patches/BASE <<EOF
base_sha=$BASE_SHA
base_full_sha=$BASE_FULL
base_subject=$BASE_SUBJECT
tip_sha=$HEAD_SHA
created=$(date -Iseconds)
commits=${#COMMITS[@]}
EOF

echo "合并补丁: $(wc -l < patches/tv-fork.diff) 行"
echo "补丁链  : ${#COMMITS[@]} 个"

# ---- 3. 自检：临时 worktree 里重放，确认能精确还原 ------------------------
TMPROOT=$(mktemp -d)
WT="$TMPROOT/verify"
git worktree add -q --detach "$WT" "$BASE_FULL"
cleanup() { git worktree remove --force "$WT" >/dev/null 2>&1 || true; rm -rf "$TMPROOT"; }
trap cleanup EXIT

(
  cd "$WT"
  git apply -3 "$REPO_ROOT/patches/tv-fork.diff"
  git add -A
  git -c user.name=verify -c user.email=verify@local commit -qm verify
  git rev-parse HEAD^{tree} > "$TMPROOT/squash.tree"
)
(
  cd "$WT"
  git reset -q --hard "$BASE_FULL"
  git clean -qfdx
  git am -3 "$REPO_ROOT"/patches/series/*.patch >/dev/null
  git rev-parse HEAD^{tree} > "$TMPROOT/series.tree"
)

# 期望树：HEAD 去掉 patches/（patches/ 是补丁自身的快照，不属于被补丁内容）
export GIT_INDEX_FILE="$TMPROOT/idx"
git read-tree HEAD
if [ -n "$(git ls-tree HEAD -- patches)" ]; then
  git rm -r -q --cached patches
fi
TIP_TREE=$(git write-tree)
unset GIT_INDEX_FILE

SQUASH_TREE=$(cat "$TMPROOT/squash.tree")
SERIES_TREE=$(cat "$TMPROOT/series.tree")

echo "tip tree    : $TIP_TREE"
echo "squash tree : $SQUASH_TREE"
echo "series tree : $SERIES_TREE"
if [ "$SQUASH_TREE" = "$TIP_TREE" ] && [ "$SERIES_TREE" = "$TIP_TREE" ]; then
  echo "自检通过：两种补丁都能精确还原我方代码"
else
  echo "自检失败：补丁与当前代码不一致" >&2
  exit 1
fi
