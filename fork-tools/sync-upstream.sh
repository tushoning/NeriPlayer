#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 上游更新后，把我方改动重新落到新上游上
#
#   bash fork-tools/sync-upstream.sh              # 用补丁链（保留每个提交）
#   bash fork-tools/sync-upstream.sh --squash     # 用单个合并补丁（冲突一次解决）
#   bash fork-tools/sync-upstream.sh --build      # 同步完顺便编译电视包
#
# 成功 → 当前分支变成「新上游 + 我方补丁」。
# 冲突 → 停下来，工作区保留 <<<<<<< 标记，手工改完 git add + git am --continue。
# ---------------------------------------------------------------------------
set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

MODE=series
DO_BUILD=0
for arg in "$@"; do
  case "$arg" in
    --squash) MODE=squash ;;
    --build)  DO_BUILD=1 ;;
    *) echo "未知参数: $arg" >&2; exit 2 ;;
  esac
done

[ -f patches/tv-fork.diff ] || { echo "找不到 patches/tv-fork.diff，先跑 fork-tools/make-patches.sh" >&2; exit 1; }
if [ -n "$(git status --porcelain)" ]; then
  echo "工作区不干净，先提交或 stash：" >&2
  git status --short >&2
  exit 1
fi

# 上游远端自动识别
UPSTREAM_REMOTE=""
for r in upstream origin; do
  url=$(git remote get-url "$r" 2>/dev/null || true)
  case "$url" in
    *cwuom/NeriPlayer*) UPSTREAM_REMOTE=$r; break ;;
  esac
done
if [ -z "$UPSTREAM_REMOTE" ]; then
  echo "找不到上游远端。先执行：" >&2
  echo "  git remote add upstream https://github.com/cwuom/NeriPlayer.git" >&2
  exit 1
fi

echo "== 拉取上游（$UPSTREAM_REMOTE）=="
git fetch --quiet "$UPSTREAM_REMOTE" master
NEW_UPSTREAM=$(git rev-parse "$UPSTREAM_REMOTE/master")
OLD_BASE=$(sed -n 's/^base_full_sha=//p' patches/BASE)
echo "补丁基线: $(git rev-parse --short "$OLD_BASE")"
echo "新上游  : $(git rev-parse --short "$NEW_UPSTREAM")  $(git log -1 --format=%s "$NEW_UPSTREAM")"

if git merge-base --is-ancestor "$NEW_UPSTREAM" "$OLD_BASE"; then
  echo "上游没有新提交，无需同步。"
  exit 0
fi

BRANCH="sync/$(date +%Y%m%d-%H%M)"
git checkout -B "$BRANCH" "$NEW_UPSTREAM"

echo
echo "== 应用我方改动（$MODE）=="
set +e
if [ "$MODE" = squash ]; then
  git apply -3 patches/tv-fork.diff
  RC=$?
else
  git am -3 --keep-cr patches/series/*.patch
  RC=$?
fi
set -e

if [ "$RC" -ne 0 ]; then
  cat <<'EOF'

------------------------------------------------------------------
冲突了。这不是失败，是上游动到了我们改过的地方。

处理方式：
  git status                  # 看 <<<<<<< ======= >>>>>>> 标记
  ...编辑冲突文件...
  git add <文件>
  git am --continue           # （--squash 模式改成 git commit）

想全部放弃重来：
  git am --abort              # （--squash 模式：git checkout -- . && git clean -fd）
  git checkout main

常见冲突点的处理原则：
  1. import 冲突      → 取并集
  2. 上游只是改名（CoreCommonR→AppR 之类）→ 取我方，再全局改名
  3. 上游真重构了     → 取上游，再把我们的 TV 逻辑补回去
------------------------------------------------------------------
EOF
  exit 1
fi

if [ "$MODE" = squash ]; then
  git add -A
  git commit -qm "feat(tv): 同步上游 $(git rev-parse --short "$NEW_UPSTREAM") 并叠加 TV 改动"
fi

echo
echo "== 同步完成 =="
git log --oneline "$NEW_UPSTREAM"..HEAD | head -10

if [ "$DO_BUILD" = 1 ]; then
  echo
  echo "== 编译电视包 =="
  export TMPDIR="$REPO_ROOT/build/tmp"
  export TEMP="$TMPDIR"
  export TMP="$TMPDIR"
  mkdir -p "$TMPDIR"
  ./gradlew :app:assembleTvrelease
fi

echo
echo "下一步："
echo "  bash fork-tools/make-patches.sh        # 把基线更新成新上游"
echo "  git checkout main && git reset --hard $BRANCH && git push -f origin main"
