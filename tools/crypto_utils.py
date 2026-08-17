"""离线复现客户端加密时常用的小工具。

设计目标：
- 与 JS 位运算逐位对齐：>>> 使用 urshift
- 标准对称算法全部交给 pycryptodome，不要手写
- 多层解密时方便打印中间层结果

依赖：
    pip install pycryptodome
"""

from __future__ import annotations

import hashlib
import hmac
import base64
from typing import Optional

try:
    from Crypto.Cipher import AES, DES, DES3
    from Crypto.Util.Padding import pad, unpad
    _HAS_CRYPTO = True
except Exception:  # pragma: no cover - 允许仅使用位运算工具
    _HAS_CRYPTO = False


# ---------- JS 位运算对齐 ----------

def u32(x: int) -> int:
    """截断为 32 位无符号整数。"""
    return x & 0xFFFFFFFF


def urshift(x: int, n: int) -> int:
    """等价于 JS 的 x >>> n。"""
    return u32(x) >> n


def rshift(x: int, n: int) -> int:
    """等价于 JS 的 x >> n（保留符号位的算术右移）。"""
    return (u32(x) ^ 0x80000000) >> n if x & 0x80000000 else x >> n


def to_int32(x: int) -> int:
    """把 32 位无符号数解释为有符号 int32。"""
    x = u32(x)
    return x - 0x100000000 if x >= 0x80000000 else x


# ---------- 编解码 ----------

def b64decode(data: str | bytes) -> bytes:
    return base64.b64decode(data)


def b64encode(data: bytes) -> str:
    return base64.b64encode(data).decode("ascii")


def hexlify(data: bytes) -> str:
    return data.hex()


# ---------- 标准算法封装 ----------

def _require_crypto():
    if not _HAS_CRYPTO:
        raise RuntimeError("缺少 pycryptodome，请先执行: pip install pycryptodome")


def aes_decrypt(
    data: bytes,
    key: bytes,
    iv: Optional[bytes] = None,
    mode: str = "cbc",
    padding: str = "pkcs7",
) -> bytes:
    """AES 解密。mode: ecb/cbc/cfb/ofb/ctr/gcm。"""
    _require_crypto()
    if mode == "ecb":
        cipher = AES.new(key, AES.MODE_ECB)
        plain = cipher.decrypt(data)
    elif mode == "cbc":
        if iv is None:
            raise ValueError("CBC 模式需要 iv")
        cipher = AES.new(key, AES.MODE_CBC, iv)
        plain = cipher.decrypt(data)
    elif mode == "cfb":
        if iv is None:
            raise ValueError("CFB 模式需要 iv")
        cipher = AES.new(key, AES.MODE_CFB, iv, segment_size=128)
        plain = cipher.decrypt(data)
    elif mode == "ofb":
        if iv is None:
            raise ValueError("OFB 模式需要 iv")
        cipher = AES.new(key, AES.MODE_OFB, iv)
        plain = cipher.decrypt(data)
    elif mode == "ctr":
        if iv is None:
            raise ValueError("CTR 模式需要传递 nonce/iv（通常为 16 字节计数器初值）")
        from Crypto.Util import Counter
        ctr = Counter.new(128, initial_value=int.from_bytes(iv, "big"))
        cipher = AES.new(key, AES.MODE_CTR, counter=ctr)
        plain = cipher.decrypt(data)
    elif mode == "gcm":
        if iv is None:
            raise ValueError("GCM 模式需要 iv/nonce")
        cipher = AES.new(key, AES.MODE_GCM, nonce=iv)
        plain = cipher.decrypt(data)
    else:
        raise ValueError(f"不支持的 AES 模式: {mode}")

    if padding == "pkcs7":
        plain = unpad(plain, AES.block_size)
    elif padding == "none":
        pass
    else:
        raise ValueError(f"不支持的 padding: {padding}")
    return plain


def aes_encrypt(
    data: bytes,
    key: bytes,
    iv: Optional[bytes] = None,
    mode: str = "cbc",
    padding: str = "pkcs7",
) -> bytes:
    _require_crypto()
    if padding == "pkcs7":
        data = pad(data, AES.block_size)
    elif padding != "none":
        raise ValueError(f"不支持的 padding: {padding}")

    if mode == "ecb":
        cipher = AES.new(key, AES.MODE_ECB)
    elif mode == "cbc":
        if iv is None:
            raise ValueError("CBC 模式需要 iv")
        cipher = AES.new(key, AES.MODE_CBC, iv)
    elif mode == "cfb":
        if iv is None:
            raise ValueError("CFB 模式需要 iv")
        cipher = AES.new(key, AES.MODE_CFB, iv, segment_size=128)
    elif mode == "ofb":
        if iv is None:
            raise ValueError("OFB 模式需要 iv")
        cipher = AES.new(key, AES.MODE_OFB, iv)
    elif mode == "ctr":
        if iv is None:
            raise ValueError("CTR 模式需要传递 nonce/iv")
        from Crypto.Util import Counter
        ctr = Counter.new(128, initial_value=int.from_bytes(iv, "big"))
        cipher = AES.new(key, AES.MODE_CTR, counter=ctr)
    elif mode == "gcm":
        if iv is None:
            raise ValueError("GCM 模式需要 iv/nonce")
        cipher = AES.new(key, AES.MODE_GCM, nonce=iv)
    else:
        raise ValueError(f"不支持的 AES 模式: {mode}")

    return cipher.encrypt(data)


def des_decrypt(data: bytes, key: bytes, iv: Optional[bytes] = None, mode: str = "cbc", padding: str = "pkcs7") -> bytes:
    _require_crypto()
    if mode == "ecb":
        cipher = DES.new(key, DES.MODE_ECB)
        raw = cipher.decrypt(data)
    elif mode == "cbc":
        if iv is None:
            raise ValueError("CBC 模式需要 iv")
        cipher = DES.new(key, DES.MODE_CBC, iv)
        raw = cipher.decrypt(data)
    else:
        raise ValueError(f"不支持的 DES 模式: {mode}")
    return unpad(raw, DES.block_size) if padding == "pkcs7" else raw


def des3_decrypt(data: bytes, key: bytes, iv: Optional[bytes] = None, mode: str = "cbc", padding: str = "pkcs7") -> bytes:
    _require_crypto()
    if mode == "ecb":
        cipher = DES3.new(key, DES3.MODE_ECB)
        raw = cipher.decrypt(data)
    elif mode == "cbc":
        if iv is None:
            raise ValueError("CBC 模式需要 iv")
        cipher = DES3.new(key, DES3.MODE_CBC, iv)
        raw = cipher.decrypt(data)
    else:
        raise ValueError(f"不支持的 3DES 模式: {mode}")
    return unpad(raw, DES3.block_size) if padding == "pkcs7" else raw


def md5(data: bytes) -> str:
    return hashlib.md5(data).hexdigest()


def sha1(data: bytes) -> str:
    return hashlib.sha1(data).hexdigest()


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def hmac_sha256(key: bytes, data: bytes) -> str:
    return hmac.new(key, data, hashlib.sha256).hexdigest()


def hmac_md5(key: bytes, data: bytes) -> str:
    return hmac.new(key, data, hashlib.md5).hexdigest()


# ---------- 调试辅助 ----------

def dump_layers(steps: list[tuple[str, bytes]]) -> None:
    """打印多层解密每层的结果，方便与 Node 插桩值对比。"""
    for name, data in steps:
        printable = data.decode("utf-8", errors="replace")
        print(f"--- {name} ---")
        print(f"hex : {data.hex()}")
        print(f"text: {printable!r}")