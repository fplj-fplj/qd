# 起点读书 APK 本地探索记录

## 结论（当前进展）

- 主要请求签名/加密入口不在 Java 层，而是在 `libfock.so` + `libfockrt.so` 的 Fock/QuickJS 黑盒里。
- Java 侧 `AsyncMainQDHttpTask` 的 HeaderInterceptor 会调用 `FockUtil.getH(...)`，`FockUtil` 是 native 方法。
- `FockRT` 内嵌 QuickJS 运行时，默认签名脚本以 base64 形式硬编码在 `com/yuewen/fockrt/f;->e` 中。
- 该默认脚本是一个 QuickJS 字节码模块（版本 1，非 bignum），核心逻辑只有两步：
  - `i_0x8756(placeholder0)`
  - `i_0x5353(上一步结果)`
- `placeholder0` 是占位符，native 侧在实际调用前会替换为真实输入。

## 关键文件/产物

- `apk_extract/default_fockrt_f_e.bin`：从 Java 常量中解出的默认 FockRT 字节码。
- `apk_extract/default_patched16.bin`：把占位符替换为 `0123456789abcdef` 的测试字节码。
- `quickjs-2021-03-27/`：为运行该字节码而编译的 QuickJS（关闭 bignum，BC_VERSION=1），并打补丁支持直接执行/import 字节码模块。
- `quickjs-2021-03-27/fockrt`：本地调试用的 `fockrt` 模块 stub，可打印 `i_0x8756` / `i_0x5353` 的入参。

## 如何运行默认脚本

```bash
cd quickjs-2021-03-27
# 使用 stub fockrt 模块观察调用链
cat > fockrt <<'EOF'
export const i_0x8756 = x => { console.log('i_0x8756', x); return 'A'+x; };
export const i_0x5353 = x => { console.log('i_0x5353', x); return 'B'+x; };
EOF
./qjs.exe --std ../apk_extract/default_fockrt_f_e.bin
```

把字节码中的 `placeholder0` 替换成任意输入后，可以看到脚本确实把输入传给 `i_0x8756`。

## Native 侧线索

- `libfockrt.so` 中有一张 JS 函数注册表（.data.rel.ro，VA `0x115170` 起），记录 `fockrt` 模块导出的函数名与实现指针：
  - `i_0x8756` -> `0x23044`
  - `i_0x5353` -> `0x23274`
- 同一张表附近还有大量 `i_0x...` 函数名，说明 `fockrt` 模块暴露了很多 native 函数。
- `libfockrt.so` 的 `.rodata` 中有：
  - 自定义字符表（约 `0xefc00`）
  - 疑似 S 盒/置换表（约 `0xefd90`）
  - 字符串 `_res_`、`%02x%02x`、`%lu_%s%s` 等
- 这些表很可能就是 `i_0x8756` / `i_0x5353` 使用的“查表 + 异或 + 乘法”核心。

## 下一步

1. 动态：在真机/模拟器上用 Frida hook `i_0x8756`、`i_0x5353`，采集多组输入输出。
2. 静态：继续逆 `0x23044` / `0x23274` 及其调用的内部函数，结合 `0xefc00` 字符表和 `0xefd90` S 盒还原算法。
3. 对拍：用还原出的 Python/Node 实现替换 stub，与 `qjs` 跑原字节码的结果逐字节比对。

## 已生成的辅助工具

- `tools/patch_fockrt_placeholder.py`：修改字节码里的 `placeholder0`，用于在本地 qjs 中传入任意输入测试。
- `tools/frida_hook_fock.js`：预留 Frida hook，用于真机/模拟器上采集 Java 层 `Fock.sign` / `FockUtil.getH`，以及后续 native 层 `i_0x8756` / `i_0x5353` 的输入输出。
