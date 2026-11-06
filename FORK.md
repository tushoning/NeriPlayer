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

### 0.4 填 4 个签名 Secret

网页路径：`仓库主页` → **Settings** → 左侧 **Secrets and variables** → **Actions** →
右上 **New repository secret**，一个一个加：

| Name（必须一模一样） | Value 从哪来 |
| --- | --- |
| `AERIAL_KEYSTORE_B64` | 见下面 0.4.1 |
| `AERIAL_STORE_PASSWORD` | `C:/Users/Tushon/keystores/aerial/README.md` 里的 `storePassword` |
| `AERIAL_KEY_PASSWORD` | 同一个文件里的 `keyPassword` |
| `AERIAL_CERT_SHA256`（可选但强烈建议） | `4ca292129ad97359a262d5f5269e7fd7ef0287b33cd19f6e761dd671946581b5` |

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

> 不开这一步，最后创建 Release 会报 403（没权限）。

### 0.6 手动跑一次

网页路径：仓库顶部 **Actions** → 左侧 **「同步上游并发布 TV 版」** →
右边 **Run workflow** → 绿色 **Run workflow** 按钮。

等 15～25 分钟（第一次要下依赖、编 native），跑完去 **Releases** 看，应该有一个
`NeriPlayer-TV.apk`。

### 0.7 电视上装

**第一次必须卸载重装**：以前电视上那个包是 debug 签名，现在换成 aerial 签名，
签名不一样不能覆盖安装。

```bash
adb uninstall moe.ouom.neriplayer.debug
adb install -r NeriPlayer-TV.apk
```

卸载会清掉应用数据（歌单缓存、登录态要重来）。之后就都是 aerial 签名了，
以后可以直接覆盖安装。

---

## 1. 分支和目录

| 分支 / 目录 | 是什么 |
| --- | --- |
| `main` | **我方完整代码**（上游 + TV 改动 + 补丁快照 + 脚本），仓库默认分支 |
| `master` | 上游原样镜像，只 `git fetch` + 快进，永不提交（本地才有，不用推） |
| `archive/*` | 历史存档，只读 |
| `patches/` | 上游补丁快照，自动生成，**不要手改** |
| `fork-tools/` | 同步与生成补丁的脚本 |
| `.github/workflows/fork-sync.yml` | CI：定时同步上游 → 编译 → 发 Release |

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

**正常情况下什么都不用做** —— CI 每 6 小时看一次上游，有新提交就自动叠补丁、
编译、发 Release、把结果推回 `main`。你只要去 Releases 下载。

**CI 红了（打不开 / 报错）** 说明上游动到了我们改过的地方，会在 Issues 里开一条记录，
列出冲突文件。本地这样处理：

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

**不要做的事**：改成 MIT 发布；只发 APK 不给源码；宣称是官方版；
用「本软件版权归我，禁止转载」这类话。

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

**Q：Release 里的包和电视上装的对不上 / 装不上**
A：先确认签名。`AERIAL_CERT_SHA256` 配了的话 CI 会在编译前校验，签名不对直接失败。

**Q：定时任务没跑**
A：① 默认分支是不是 `main`；② 仓库 60 天没提交会被暂停 schedule，
去 Actions 页点 **Enable workflow**；③ schedule 是「尽力而为」，高峰会延后 1～2 小时。

**Q：想让电视上的 app 自动检查更新**
A：那是另一套东西（应用内更新指向本仓库 Release）。本 fork 目前没做，
要做得在 `app` 里加更新检查逻辑指向 `tushoning/NeriPlayer/releases/latest`。

**Q：`git push` 提示 rejected**
A：CI 会用 `--force-with-lease` 推 `main`，你本地落后了。先 `git pull --rebase`，
或者从 CI 那个 commit 重新开始。
