#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 上游更新后，把我方改动重新落到新上游上
#
#   bash tools/sync-upstream.sh            # 用补丁链（git am -3），保留提交信息
#   bash tools/sync-upstream.sh --squash   # 用单个合并补丁（冲突一次解决）
#   bash tools/sync-upstream.sh --build    # 同步完顺便编译 TV release
#
# 成功 → 当前分支变成「新上游 + 我方补丁」，直接 push 即可。
# 冲突 → 停下来，工作区保留冲突标记，按提示手工解决后 git am --continue。
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

[ -f patches/tv-fork.diff ] || { echo "找不到 patches/tv-fork.diff，先跑 tools/make-patches.sh" >&2; exit 1; }
if [ -n "$(git status --porcelain)" ]; then
  echo "工作区不干净，先提交或 stash：" >&2
  git status --short >&2
  exit 1
fi

echo "== 拉取上游 =="
git fetch origin master
NEW_UPSTREAM=$(git rev-parse origin/master)
OLD_BASE=$(git rev-parse patch-base 2>/dev/null || sed -n 's/^base_full_sha=//p' patches/BASE)
echo "补丁基线: $(git rev-parse --short "$OLD_BASE")"
echo "新上游  : $(git rev-parse --short "$NEW_UPSTREAM")  $(git log -1 --format=%s "$NEW_UPSTREAM")"

if [ "$OLD_BASE" = "$NEW_UPSTREAM" ]; then
  echo "上游没有新提交，无需同步。"
  exit 0
fi

if git merge-base --is-ancestor "$NEW_UPSTREAM" "$OLD_BASE"; then
  echo "上游没有新提交（本地基线更新），无需同步。"
  exit 0
fi

BRANCH="sync/$(date +%Y%m%d-%H%M)"
git branch -f "$BRANCH" "$NEW_UPSTREAM" 2>/dev/null || true
git checkout -B "$BRANCH" "$NEW_UPSTREAM"

echo
echo "== 应用我方改动（$MODE）=="
set +e
if [ "$MODE" = squash ]; then
  git apply -3 patches/tv-fork.diff
  RC=$?
else
  git am -3 patches/series/*.patch
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
  git checkout our-changes

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
  git -c user.name="$(git log -1 --format=%an our-changes)" \
      -c user.email="$(git log -1 --format=%ae our-changes)" \
      commit -qm "feat(tv): 同步上游 $(git rev-parse --short "$NEW_UPSTREAM") 并叠加 TV 改动"
fi

echo
echo "== 同步完成 =="
git log --oneline "$NEW_UPSTREAM"..HEAD | head -20
echo
echo "下一步："
echo "  git diff $OLD_BASE HEAD --stat        # 复核改动没被吃掉"
echo "  bash tools/make-patches.sh            # 重新生成补丁快照（基线变成新上游）"
echo "  git checkout our-changes && git reset --hard $BRANCH   # 接受这次同步"

if [ "$DO_BUILD" = 1 ]; then
  echo
  echo "== 编译 TV release =="
  export JAVA_HOME="${JAVA_HOME:-$REPO_ROOT/../toolchain/jdk/jdk-17.0.20.1+1}"
  export PATH="$JAVA_HOME/bin:$PATH"
  export TMPDIR="$REPO_ROOT/build/tmp"
  export TEMP="$REPO_ROOT/build/tmp"
  export TMP="$REPO_ROOT/build/tmp"
  mkdir -p "$TMPDIR"
  ./gradlew :app:assembleTvrelease -g "$REPO_ROOT/../toolchain/gradle-home"
fi
