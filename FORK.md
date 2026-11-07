# 这个 fork 怎么维护

上游：[cwuom/NeriPlayer](https://github.com/cwuom/NeriPlayer)（Android 音乐播放器，手机版 UI）。
本 fork：**<https://github.com/tushoning/NeriPlayer>**，唯一目的是**给电视做适配**
（侧栏导航、遥控焦点、去动效、电视布局）。

---

## 0. 第一次上传：照着点就行

### 0.1 在 GitHub 上建 fork

1. 浏览器打开 <https://github.com/cwuom/NeriPlayer>
2. 右上角 **Fork** → **Create fork**
3. 得到 `https://github.com/tushoning/NeriPlayer`（名字保持 `NeriPlayer`）

### 0.2 把本机代码推上去

在这台机器上、仓库目录里执行（Git Bash）：

```bash
cd C:/Users/Tushon/WorkBuddy/2026-10-24-00-27-15/work/neri-fork
git push -f fork main:main
```

- `fork` 这个远端已经配好，指向 `https://github.com/tushoning/NeriPlayer.git`
- `-f` 是强推：fork 出来的仓库带的是上游原版，我们要用本机这份覆盖掉
- 首次 push 会弹浏览器登录 GitHub，授权一次就好

### 0.3 把默认分支改成 main

刚 fork 出来的仓库默认分支是 `master`（上游原样）。**改掉它，否则定时任务不会跑。**

网页路径：
`仓库主页` → **Settings**（设置，顶部菜单最右）→ 左侧 **General** →
**Default branch** → 点 `master` 右边的双向箭头图标 → 选 `main` → **Update**

> 为什么必须改：GitHub Actions 的定时任务（schedule）只在**默认分支**上生效。

### 0.4 填 3 个签名 Secret

网页路径：`仓库主页` → **Settings** → 左侧 **Secrets and variables** → **Actions** →
右上 **New repository secret**，一个一个加：

| Name（必须一模一样） | Value 从哪来 |
| --- | --- |
| `AERIAL_KEYSTORE_B64` | 见下面 0.4.1 |
| `AERIAL_STORE_PASSWORD` | `C:/Users/Tushon/keystores/aerial/README.md` 里的 `storePassword` |
| `AERIAL_KEY_PASSWORD` | 同一个文件里的 `keyPassword` |

只需要这 3 个。证书指纹（`4ca2921…581b5`）是公开信息、不是密钥，
已经写死在 workflow 的 `CERT_SHA256` 里，不用再配。

#### 0.4.1 把密钥库转成 base64 文本

在 Git Bash 里跑：

```bash
cd /c/Users/Tushon/keystores/aerial
base64 -w0 aerial.keystore > aerial.b64
cat aerial.b64
```

终端会吐出一长串字符（约 3.6 KB，一整行，没有换行）。**全选复制**，
粘贴到 `AERIAL_KEYSTORE_B64` 的 Value 里。粘完可以删掉 `aerial.b64`。

> 注意：`.gitignore` 已经把 `*.keystore` / `*.b64` 挡住，这些文件不会被提交上去。

### 0.5 打开 Actions 的写权限

网页路径：**Settings** → 左侧 **Actions** → **General** → 拉到最下面
**Workflow permissions** → 选 **Read and write permissions** → **Save**。

> 工作流文件里已经写了 `permissions: contents: write`，正常情况下不开也能跑。
> 但这里开一下更保险——万一 GitHub 那边策略优先级不同，不开会卡在推分支 / 建 Release（403）。
>
> 这一项只影响我写的那个工作流能不能动仓库，不会让上游那些 CI 跑起来（它们只认 `master`）。

### 0.6 手动跑一次

网页路径：仓库顶部 **Actions** → 左侧 **「同步上游并发布 TV 版」** →
右边 **Run workflow** → 绿色 **Run workflow** 按钮。

**手动触发总是出包**：不管有没有新 Release，都会用当前 `main` 的代码编一遍并发 Release。
定时触发只在**上游发了新 Release** 时才出包（上游 master 天天有提交也不管）。

等 15～25 分钟（第一次要下依赖、编 native），跑完去 **Releases** 看，应该有一个
`NeriPlayer-TV.apk`。tag 形如 `tv-20261107-0205-1ce32a4`（日期-时分-上游短哈希），
同一个 tag 只对应一次构建，想重编就再点一次，会生成新 tag。

### 0.7 电视上装

包名是 **`moe.ouom.neriplayer.tv`**，和电视上原来那个 `moe.ouom.neriplayer.debug`
是两个不同的应用，可以直接共存安装（不会覆盖、也不会继承旧包的数据）。

建议把旧的卸掉，免得桌面出现两个图标：

```bash
adb uninstall moe.ouom.neriplayer.debug   # 卸掉旧的调试包
adb install -r NeriPlayer-TV.apk          # 装新的 .tv 包
```

旧包的数据（歌单缓存、登录态）不会迁移过来，需要重新设置。之后就都是 aerial 签名 +
`.tv` 包名了，以后可以直接覆盖安装。

---

## 1. 分支和目录

| 分支 / 目录 | 是什么 |
| --- | --- |
| `main` | **我方完整代码**（上游 + TV 改动 + 补丁快照 + 脚本），仓库默认分支 |
| `master` | 上游原样镜像，只 `git fetch` + 快进，永不提交（本地才有，不用推） |
| `archive/*` | 历史存档，只读 |
| `patches/` | 上游补丁快照，自动生成，**不要手改** |
| `fork-tools/` | 同步与生成补丁的脚本 |
| `.github/workflows/fork-sync.yml` | CI：定时检查上游 Release → 有新版才编译 → 发 Release |

**`main` 必须保持线性**（不含 merge commit）。`patches/series/` 是靠逐个 `git am`
重放的，历史里一旦有 merge，merge 之后的提交其 diff 基准是合并结果而不是上游，
拆出来的补丁就装不回去。

> 2026-11-06 之前的历史里有一次 merge（`bb54fb58`），已经用等价线性链替换，
> 旧历史存在 `archive/our-changes-20261106` 和标签 `pre-linear-20261106`。

---

## 2. 改了代码之后

```bash
git add -A && git commit            # 正常提交到 main
bash fork-tools/make-patches.sh     # 重新生成 patches/ 快照
git push origin main
```

`make-patches.sh` 做三件事：

1. 生成 `patches/tv-fork.diff`（单个合并补丁）
2. 生成 `patches/series/NNNN-<sha>.patch`（按提交拆分的补丁链）
3. **在临时 worktree 里把两种补丁各重放一遍，比对 tree 哈希是否与当前代码完全一致**

第 3 步没通过就报错退出，不会让你拿着坏补丁去同步。

---

## 3. 上游更新了怎么办

**正常情况下什么都不用做** —— CI 每小时去上游看**有没有发新 Release**：

| 情况 | 动作 |
| --- | --- |
| 有新 Release | 把我方补丁叠到上游 master → 编译 → 发 Release → 推回 `main` → 打 `seen/<Release 名>` 标记 |
| 没有新 Release | 几秒钟退出，不编译（上游 master 中间那些提交不会触发打包） |

所以出包节奏跟着上游发版走，大约 3～5 天一个。

> ⚠️ `seen/*` 那些 tag 是「这个上游 Release 已经处理过」的记号，**千万别删**——删了下一次定时任务会当成新 Release，重复出一个包。`tv-*` 的 tag 更不能删（GPL 要求），详见第 4 节。

**CI 红了（打不开 / 报错）** 说明上游动到了我们改过的地方。GitHub 会给仓库 owner
发一封「workflow 运行失败」的通知（手机 App 的 Inbox 也能收到），点进去那次运行，
**最上方的 Summary 区**会列出冲突文件。本地这样处理：

```bash
bash fork-tools/sync-upstream.sh --build
```

- **没冲突**：分支变成「新上游 + 我方补丁」，然后

  ```bash
  bash fork-tools/make-patches.sh
  git checkout main && git reset --hard sync/<日期>
  git push -f origin main
  ```

- **有冲突**：脚本停下并保留 `<<<<<<<` 标记，按这个表处理

  | 冲突类型 | 处理 |
  | --- | --- |
  | import 列表 | 取并集 |
  | 上游只是重命名（`CoreCommonR`→`AppR` 之类） | 取我方，再全局改名 |
  | 上游真重构了 | 取上游，再把 TV 逻辑补回去 |

  改完 `git add <文件>` → `git am --continue`。想全放弃：`git am --abort`。

只想一次解决完冲突、不要分提交：`bash fork-tools/sync-upstream.sh --squash`。

---

## 4. 许可证：GPL-3.0-or-later

根目录 `LICENSE` 是 GPLv3 全文，源码文件头写的是「either version 3 … or any later
version」，所以整个项目是 **GPL-3.0-or-later**。`THIRD_PARTY_LICENSES.md` 里还有
Capsulyric（GPL-3.0-or-later）、InstallerX Revived（GPL-3.0-only）、libusb（LGPL-2.1-or-later），
跟着各自的许可证走。

发布 APK 要守的几条：

| GPLv3 条款 | 要求 | 本仓库怎么做的 |
| --- | --- | --- |
| §5a | 修改版要有醒目改动声明 + 日期 | 根目录 `NOTICE`，README 顶部那段，Release 说明 |
| §5c | 整个作品整体继续按 GPL 授权 | 保留 `LICENSE`，不加任何额外限制 |
| §6 | 发二进制要给**对应源码** | Release 的 tag 打在编译的那个 commit 上，GitHub 自动给 tag 附源码包 |
| §5d | 保留原版权声明与无担保声明 | 文件头不动 |
| §10 | 不得追加额外限制 | 不写「禁止商用/转载」之类 |
| §7 | 保留上游署名 | README、NOTICE 都指向上游 |

### 四个真正容易踩的坑

1. **别删 tag、别删 Release。** 删了 tag，GitHub 的源码包跟着没了，已经发出去的 APK 就失去
   对应源码——这是唯一会真正造成违规的操作。`tv-*` 的 tag 一个都别动。

2. **别加任何额外限制。** 国内安卓圈习惯写「仅供学习交流，禁止转载/商用/二次分发」、
   「下载即视为同意本协议」——这些话和 GPLv3 §10（不得对接收者施加额外限制）**直接冲突**，
   写了就等于自己把授权变成非自由许可。可以写的是：这是 fork、不是官方、无担保。

3. **GitHub 的源码包不含子模块。** 这个项目构建依赖 `np-submodule/accompanist-lyrics-core`
   和 `accompanist-lyrics-ui`，GitHub 给 tag 自动打的 zip 里它们是空目录。
   所以 Release 说明里必须列出子模块地址和 commit（CI 已经自动写进去），
   别人才能真的复现出这个 APK。

4. **GPL 不可撤回。** 一旦发布了某个版本，任何人拿到后都可以按 GPL 继续分发，
   你之后想收回、想改成闭源都做不到。发之前想清楚。

### 可以做的事（常见误解）

- ✅ 公开仓库、发 Releases、发网盘、发群里 —— 只要同时能看到源码
- ✅ 换应用名、换图标、改包名
- ✅ 收费（GPL 允许卖二进制），但买了的人有权拿到源码并再分发
- ✅ 只发 APK 不附源码文件本身 —— 前提是**同一个地方**能拿到源码（本仓库 tag 即可）
- ❌ 只发 APK 且不提供任何源码获取方式
- ❌ 在 Release 里写「禁止转载」「版权归我所有」

**不要做的事**：改成 MIT 发布；宣称是官方版；用上游作者名义发版（商标不在 GPL 授权范围）。

---

## 5. 签名

所有自建应用统一用这一把 key：

| 项 | 值 |
| --- | --- |
| 密钥库 | `C:/Users/Tushon/keystores/aerial/aerial.keystore`（PKCS12）|
| alias | `aerial` |
| 证书 SHA-256 | `4ca292129ad97359a262d5f5269e7fd7ef0287b33cd19f6e761dd671946581b5` |
| 有效期 | 到 2056-10-16 |

密码在同目录 `README.md` 里，**不要写进本仓库**（仓库是公开的）。

### 和上游 `CONTRIBUTING.md`「构建发布版」的关系

上游文档那一段是写给**自己编 release 的贡献者**看的，作用有三个：

1. 说清楚签名参数怎么传（`KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`
   四个 Gradle 属性，`KEYSTORE_FILE` 写相对路径时按 `app/` 模块目录解析）；
2. 说清楚产物在哪（`app/build/outputs/apk/release/`，命名 `NeriPlayer-<短哈希>.<MMddHHmm>.apk`）；
3. 隐含一条规矩：**上游不会给你签名密钥**，官方发布包的签名只有上游自己有，
   任何人自编的包都不可能覆盖安装官方版。

我们的做法和它一致（属性名、相对路径规则、产物命名全对齐）。

唯一分歧：上游的 `release` 变体「不会回退到 debug signing config，没密钥就直接报错」，
而本 fork 的 `tvrelease` 变体在**没有正式密钥时回退 debug 签名**——这是为了本地没密钥也能快速迭代。
发出去的包不受影响：CI 里「密钥库指纹」和「APK 签名指纹」两道校验都过硬才允许发 Release，
哪一步不对都会直接失败，不会出现「用 debug 签名发出去、电视装不上」的情况。

### 本地出带签名的包

```bash
./gradlew :app:assembleTvrelease \
  -PKEYSTORE_FILE=C:/Users/Tushon/keystores/aerial/aerial.keystore \
  -PKEYSTORE_PASSWORD=<store 密码> \
  -PKEY_ALIAS=aerial \
  -PKEY_PASSWORD=<key 密码>
```

不传 `-P` 时会退回 debug 签名（本地随便跑跑够用，但发出去的包必须用上面的）。

### 想让所有仓库都不用重复配 secret

GitHub 的 Secret 分三级，**只有 Organization secrets 是「配一次全仓库可用」**：

- Repository secrets / Environment secrets：每个仓库各配一遍
- **Organization secrets**：组织里所有仓库都能引用

前提：仓库要放在一个 **Organization** 下（个人账号没有组织级 secret），
而且免费计划下组织级 secret **只能给 public 仓库用**（私有仓库要 Team）。

配置路径：`组织主页` → **Settings** → **Secrets and variables** → **Actions** →
**New organization secret** → Name 填 `AERIAL_KEYSTORE_B64` → Repository access 选
**All repositories**。

名字保持 `AERIAL_*` 不变，其他仓库的 workflow 直接 `${{ secrets.AERIAL_KEYSTORE_B64 }}` 就能用。

---

## 6. 常见问题

**Q：GitHub 仓库页上那个「Sync fork」按钮要不要点？**
A：**不要点。** 一次都不要。

那个按钮做的事是把上游的提交 **merge 进你的分支**。只要 `main` 上有你自己的提交
（我们有一堆），它就一定产生一个 merge commit，而 `patches/series/` 是靠
`git am` 逐个重放的，merge 之后的提交其 diff 基准是合并结果而不是上游，补丁立刻错乱。
2026-11-06 之前的历史里就有一个这样的 merge（`bb54fb58`），当时拆补丁拆不出来，
只能整条历史重建一次。

更危险的是同一个位置还有一个 **「Discard N commits」**——点了会把你所有 TV 改动
直接删掉，回到上游原样。

**Q：仓库页左上角的「Contribute」要不要点？**
A：**不要点。** 那个按钮 = 向上游 `cwuom/NeriPlayer` 提 Pull Request，
会把你的 21 个提交原样提交给上游作者审。这是本地适配 fork，不是给上游的贡献：
改了包名后缀、加了 aerial 签名配置、加了 `FORK.md` / `patches/` / CI，
上游不会接受，也给人添麻烦。

将来如果发现某个 bug 是上游通用问题（比如焦点逻辑本身的缺陷），
可以从那次修复里单独摘一个 commit 提 PR —— 那属于另一件事，到时候再说。

**Q：仓库右侧的「About」要不要改？**
A：建议改。fork 出来的 About 默认抄的上游描述，别人搜到会以为是官方版。
点 About 右边的齿轮，填：

- **Description**：`NeriPlayer 的电视适配 fork（Android TV / 遥控器）：侧栏导航、遥控焦点、精简动效。非上游官方版本。`
- **Website**：留空
- **Topics**：`android-tv` `android` `tv` `music-player` `kotlin` `jetpack-compose` `tv-fork`

不改也不影响任何功能，纯粹是别让人误会。

上游更新交给两样东西就够了：
- 平时：CI 每小时自动做（`fork-sync.yml`）
- 本地手动：`bash fork-tools/sync-upstream.sh`

**Q：为什么手动跑的时候上游没更新就不出包？**
A：已经改了。现在**手动触发（Run workflow）总是出包**，不管有没有新 Release，
都会用当前 `main` 的代码编一遍并发 Release；只有定时触发才遵守「没新 Release 就跳过」。

**Q：为什么上游明明有新提交，CI 却一直绿着不出包？**
A：**这是设计如此**。定时任务只看上游有没有**发 Release**，不看 master 提交数。
上游大约 3～5 天发一版，中间天天有提交也不出包——避免把上游半成品打成电视包，
也省 CI 时间。上游发新版后最多 1 小时就会自动出包。
想立刻要一个包，手动点 **Run workflow** 即可。

**Q：仓库里那些 `seen/xxx` 的 tag 是什么？**
A：**「这条上游 Release 已经打包过」的记号**，由 CI 在整条流程全部成功后自动打。
定时任务靠它判断「有没有新 Release」。**不要删**，删了会重复出一个包。
它和 `tv-*` 是两回事，后者是真正发布用的版本 tag（GPL 要求必须留）。

**Q：CI 失败怎么通知我？会往 Issues 里发东西吗？**
A：**不会开 Issue**，仓库里一个 Issue 都不会有。失败通知只走 GitHub 自己的渠道
（邮件 + 手机 App 的 Inbox），点进那次运行，最上方 **Summary** 区就是冲突详情。

**Q：`np-submodule/` 里头模块的改动提交到哪了？**
A：**没有提交到任何地方**——那是上游别人的仓库，我们推不上去。本地那几份文件一直是「工作区裸文件」状态。
主仓库在 `patches/submodule/` 存了一份改后的整文件，CI 编译前会覆盖回子模块目录
（否则 app 里调用 `linesInteractive` 参数会报「No parameter with name ... found」）。

本地如果执行过 `git submodule update --init --recursive`，把改动冲掉了，跑这条恢复：

```bash
cp -r patches/submodule/accompanist-lyrics-ui/. np-submodule/accompanist-lyrics-ui/
```

> 目前只有 `accompanist-lyrics-ui/…/KaraokeLyricsView.kt` 一个文件有改动
> （加 `linesInteractive` 参数 + 亚像素滚动容差）。其余三个子模块与上游一致。

**Q：Release 里的包和电视上装的对不上 / 装不上**
A：CI 有两道校验——密钥库指纹（编译前）和 APK 签名指纹（发布前），
签名不对会直接失败，不会发出装不上的包。

**Q：定时任务没跑**
A：① 默认分支是不是 `main`；② 仓库 60 天没提交会被暂停 schedule，
去 Actions 页点 **Enable workflow**；③ schedule 是「尽力而为」，高峰会延后 1～2 小时。

**Q：想让电视上的 app 自动检查更新**
A：那是另一套东西（应用内更新指向本仓库 Release）。本 fork 目前没做，
要做得在 `app` 里加更新检查逻辑指向 `tushoning/NeriPlayer/releases/latest`。

**Q：`git push` 提示 rejected**
A：CI 会用 `--force-with-lease` 推 `main`，你本地落后了。先 `git pull --rebase`，
或者从 CI 那个 commit 重新开始。
