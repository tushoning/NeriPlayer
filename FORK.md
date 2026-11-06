# 这个 fork 怎么维护

上游：[cwuom/NeriPlayer](https://github.com/cwuom/NeriPlayer)（Android 音乐播放器，手机版 UI）。
本 fork 的唯一目的：**给电视做适配**（侧栏导航、遥控器焦点、去动效、TV 布局、TV release 变体）。

---

## 1. 分支约定

| 分支 | 内容 | 规则 |
| --- | --- | --- |
| `master` | 上游原样镜像 | 只 `git fetch` + 快进，永不提交 |
| `our-changes` | 上游 + 我方改动 | **必须线性**（不含 merge commit） |
| `archive/*` | 历史存档 | 只读，出问题回这里翻 |

「必须线性」不是洁癖：`patches/series/` 是靠逐个 `git am` 重放的，历史里一旦有 merge，
merge 之后的提交其 diff 基准是合并结果而不是上游基线，拆出来的补丁就装不回去。
2026-11-06 之前的 `our-changes` 就有一次 merge（`bb54fb58`），已经用等价的线性链替换，
旧历史存在 `archive/our-changes-20261106` 和标签 `pre-linear-20261106`。

---

## 2. 改了代码之后

```bash
git add -A && git commit            # 正常提交到 our-changes
bash fork-tools/make-patches.sh          # 重新生成 patches/ 快照
```

`make-patches.sh` 会做三件事：

1. 生成 `patches/tv-fork.diff`（单个合并补丁）
2. 生成 `patches/series/NNNN-<sha>.patch`（按提交拆分的补丁链）
3. **在一个临时 worktree 里把两种补丁各重放一遍，比对 tree 哈希是否与当前代码完全一致**

第 3 步没通过就直接报错退出，不会让你拿着坏补丁去同步。生成失败基本都是因为
`patches/` 被算进了补丁内容 —— 脚本已经用 pathspec 排除，别手动改那一行。

---

## 3. 上游更新了，怎么合

```bash
bash fork-tools/sync-upstream.sh --build
```

脚本做的事：`git fetch origin master` → 在新上游上开 `sync/<日期>` 分支 →
`git am -3 --keep-cr patches/series/*.patch` → 编译。

- **没冲突**：分支直接变成「新上游 + 我方补丁」，`git diff` 复核后
  `git checkout our-changes && git reset --hard sync/<日期>` 接受，
  再跑一次 `make-patches.sh` 把基线更新成新上游。
- **有冲突**：脚本会停下并保留冲突标记，工作区里能看到 `<<<<<<<`。
  按老规矩处理：

  | 冲突类型 | 处理 |
  | --- | --- |
  | import 列表 | 取并集 |
  | 上游只是重命名（`CoreCommonR`→`AppR` 之类） | 取我方，再全局改名 |
  | 上游真重构了 | 取上游，再把 TV 逻辑补回去 |

  改完 `git add <文件>` → `git am --continue`。想全放弃：`git am --abort`。

只想一次解决完冲突、不要分提交：`bash fork-tools/sync-upstream.sh --squash`。

> Windows 上 `core.autocrlf=true` 会让补丁的 LF 上下文行失配。
> 仓库根的 `.gitattributes` 已经把 `*.patch` / `*.diff` 标记成 `-text`，别删。

---

## 4. 许可证：GPL-3.0-or-later，以及发布 Release 要做什么

**不是 MIT，是 GPL。** 根目录 `LICENSE` 是 GPL-3.0 全文，源码文件头写的是
「either version 3 of the License, or (at your option) any later version」，
所以整个项目是 **GPL-3.0-or-later**。`THIRD_PARTY_LICENSES.md` 里还有
Capsulyric（GPL-3.0-or-later）、InstallerX Revived（GPL-3.0-only）、libusb（LGPL-2.1-or-later）
等第三方代码，这些跟着各自的许可证走，不能被我方改动覆盖。

### fork + 自动打包发 Release，闭环要满足这几条

| GPLv3 条款 | 要求 | 在 GitHub 上怎么落地 |
| --- | --- | --- |
| §5a | 修改版必须有**醒目的改动声明 + 修改日期** | 见下方 `NOTICE` 模板 + Release 说明 |
| §5c | 整个作品（含你的修改）**整体继续按 GPL 授权**，不能只给补丁 | 仓库根保留 `LICENSE`，不要改 |
| §6 | 分发 APK（object code）时必须让接收者拿到**对应源码**（指编译这个 APK 的确切版本，含构建脚本） | Release 的 tag 必须就是编译用的那个 commit；GitHub 会自动为 tag 附 `Source code (zip/tar.gz)` —— 这就是"同一地点取得源码" |
| §5d | 保留原有的版权声明与无担保声明 | 不改文件头，不动 `LICENSE` |
| §10 | 不得对接收者追加额外限制 | 别加「禁止商用」「禁止二次分发」之类条款 |
| §7 | 保留上游作者署名 | README 里写清楚 fork 自谁 |

**具体做法（这套已经能直接用于自动发布）：**

1. **每个 Release 的 tag 就是源码版本**。CI 只用「已打 tag 的 commit」编译，
   绝不从工作区随手打包。这样 §6 自动满足，因为 GitHub 给 tag 附源码。
2. **Release 说明里写清 fork 关系与改动**：

   ```markdown
   ## 本版本
   基于上游 cwuom/NeriPlayer <上游sha> 构建。

   ## 与上游的差异
   本构建是电视适配 fork，修改内容见仓库 `patches/tv-fork.diff`，
   修改日期 2026-11-06。主要改动：TV 侧栏导航、遥控器焦点、
   去除部分动效、TV release 变体。

   ## 源码
   与本次 APK 完全对应的源码：本仓库 tag `<tag>`（GPL-3.0-or-later）。
   上游项目：https://github.com/cwuom/NeriPlayer

   ## 许可
   本程序为自由软件，按 GNU General Public License v3.0 或更高版本发布，
   无任何担保。LICENSE 全文见仓库根目录。
   ```

3. **加一个 `NOTICE` 文件**（§5a 的「醒目声明」）：

   ```
   This is a modified version of NeriPlayer.
   本程序是 NeriPlayer 的修改版（电视适配 fork）。

   Upstream: https://github.com/cwuom/NeriPlayer
   Modifications: TV (television) UI adaptation — sidebar navigation,
   D-pad focus handling, reduced animations, TV release build variant.
   Modified: 2026-11-06
   Full diff: patches/tv-fork.diff
   License: GPL-3.0-or-later (see LICENSE)
   ```

4. **不要做的事**：
   - 不要改成 MIT / Apache 发布，哪怕只发 APK。
   - 不要在 Release 里写「本软件版权归我，禁止转载」之类的附加限制。
   - 不要宣称是官方版本、不要用上游作者名义发版（商标不在 GPL 授权范围）。
     应用内标题建议带个 fork 标记，避免和官方版混淆。
   - 不要只发 APK 不给源码链接（§6 违规）。

5. **`applicationId` 保持 `moe.ouom.neriplayer`**：GPL 不要求改包名，
   改了反而装不上、丢数据。合规义务在「源码可得 + 改动声明」，不在包名。

> 一句话记：**发布 APK 的同时，让任何人能从这个仓库的这个 tag 拿到一模一样的源码。
> 做到这一点，剩下的就是把 LICENSE 留着、把改动说清楚。**

---

## 5. GitHub Actions 签名：一套 key 怎么给所有仓库用

### 先明确：一把 key 签所有 app 是可行的

Android 签名只校验「同一个包名的两次安装是否同一把 key」，不同包名之间互不影响。
所以一把个人发布 key 签你所有 app 技术上完全没问题。代价是**单点风险**：
key 泄露 = 所有 app 都能被人伪造升级；一把 key 用在 Google Play 还会互相牵连
（Play 上架要每个应用独立的上传密钥）。个人 sideload 分发的话，一把就够。

### 关键限制：GitHub 的 Secrets 分三级，只有一级是「配一次全仓库可用」

| 级别 | 配置位置 | 能否跨仓库 |
| --- | --- | --- |
| Repository secrets | 每个仓库 Settings → Secrets | ❌ 要一个仓库一个仓库配 |
| Environment secrets | 每个仓库 Settings → Environments | ❌ 同上 |
| **Organization secrets** | 组织 Settings → Secrets and variables → Actions | ✅ **配一次，组织内所有仓库可用** |

**免费（GitHub Free）计划下，组织级 secret 只能给 public 仓库用**；
私有仓库要用，得升 Team（$4/人/月）。你这个 fork 本来就是 public（GPL 发布也要 public 最省事），
所以免费方案就够。

### 操作步骤

1. **把仓库放进一个组织**（个人账号下没有组织级 secrets）：
   `Settings → Transfer ownership` 转到一个你自己建的 Organization。

2. **准备 key**（如果还没统一）：

   ```bash
   keytool -genkeypair -v -keystore release.jks -alias key0 \
     -keyalg RSA -keysize 4096 -validity 36500 \
     -storetype JKS
   # 记下 store password / key password，存进密码管理器，别只留在某一台机器上
   ```

3. **base64 编码后存成组织 secret**（上游 CI 期望的名字就是这四个，直接照用）：

   ```bash
   # Windows git-bash
   base64 -w0 release.jks > release.jks.b64
   ```

   组织页面：`Organization → Settings → Secrets and variables → Actions → New organization secret`

   | Name | Value |
   | --- | --- |
   | `KEYSTORE_B64` | `release.jks.b64` 的内容 |
   | `KEYSTORE_PASSWORD` | store 密码 |
   | `KEY_PASSWORD` | key 密码 |
   | `KEY_ALIAS`（可选，上游写死 `key0`） | 别名 |

   Repository access 选 **All repositories**（私有仓库在免费计划下会拿到空值，不影响 public 的）。

   命令行等价写法：

   ```bash
   gh secret set KEYSTORE_B64 --org <你的组织> --visibility all < release.jks.b64
   gh secret set KEYSTORE_PASSWORD --org <你的组织> --visibility all
   gh secret set KEY_PASSWORD --org <你的组织> --visibility all
   ```

4. **workflow 里照常引用**，不用改任何东西：

   ```yaml
   env:
     ORG_GRADLE_PROJECT_KEYSTORE_FILE: neri.jks
     ORG_GRADLE_PROJECT_KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
     ORG_GRADLE_PROJECT_KEY_ALIAS: key0
     ORG_GRADLE_PROJECT_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
   run: |
     printf '%s' "$KEYSTORE_B64" | base64 -d > app/neri.jks
   ```

   本项目 `app/build.gradle.kts` 读的就是 `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` /
   `KEY_ALIAS` / `KEY_PASSWORD` 这四个 Gradle 属性，所以 `ORG_GRADLE_PROJECT_` 前缀一套即可。

### 备选（如果你不想建组织）

- **中心仓库 + reusable workflow**：把「解码 key → 编译 → 发布」写在一个仓库的
  `workflow_call` 工作流里，其他仓库 `uses: you/ci/.github/workflows/android-release.yml@v1`。
  但**传 secrets 仍然要调用方仓库自己持有**（`secrets: inherit` 只传调用方已有的），
  所以这条路省不掉逐个配置 —— 除非那些 secret 本身放在组织级。
- **私有「钥匙库」仓库**：key 放私有仓库，workflow 用 GitHub App token 拉下来。
  这样每个仓库只需要一个 token secret，仍然比四个少，但复杂度和泄密面都更大，不推荐。

**结论：建组织 → 配一次 Organization secrets → 所有仓库直接引用。**

---

## 6. 自动同步 + 自动发 Release

`.github/workflows/fork-sync.yml` 已配好，逻辑是：

```
每天定时 / 手动触发
  → fetch 上游
  → 新提交？没有就退出
  → git am -3 --keep-cr patches/series/*.patch
      失败 → 开 issue 列出冲突文件，停止（等人处理）
      成功 → 编译 :app:assembleRelease -PbuildAllReleaseAbis=true（组织 secret 签名）
           → 打 tag tv-<日期>-<上游sha前7位>
           → 建 Release，附 APK + Release notes（含 §4 的改动声明与源码指引）
           → push our-changes
```

> 为什么是 `assembleRelease` 而不是 `assembleTvrelease`：后者是给本地迭代用的
> ——`.debug` 包名后缀 + 调试签名，方便覆盖安装不丢数据。对外发布的包必须用
> 正式包名 + 正式签名，否则别人装不上，你自己的升级链也断在这里。
>
> 为什么必须 `--keep-cr`：上游有两个文件（`SongEncyclopediaContent.kt`、
> `SongEncyclopediaArtistSection.kt`）的内容本身就是 CRLF。`git am` 默认会把补丁行
> 末尾的 `\r` 剥掉，导致这两段 hunk 对不上，报 "patch does not apply"。
> 这不是补丁坏了，是 `git am` 的默认行为，加 `--keep-cr` 即可。

注意：Release 的 tag 一定要由 CI 打在**刚编译成功的那个 commit** 上，
GitHub 会自动给这个 tag 附源码包，这就是 §6 的合规闭环。
