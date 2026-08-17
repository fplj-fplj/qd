#!/usr/bin/env python3
"""Patch the placeholder string inside a FockRT QuickJS bytecode file.

Usage:
    python tools/patch_fockrt_placeholder.py input.bin output.bin "new input"
"""
import sys
from pathlib import Path


def read_leb(data: bytes, pos: int):
    res = 0
    shift = 0
    while True:
        b = data[pos]
        pos += 1
        res |= (b & 0x7F) << shift
        if not (b & 0x80):
            break
        shift += 7
    return res, pos


def write_leb(n: int) -> bytes:
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            break
    return bytes(out)


def find_placeholder(data: bytes, placeholder: bytes) -> tuple:
    pos = 1  # version byte
    count, pos = read_leb(data, pos)
    for _ in range(count):
        len_start = pos
        le, pos = read_leb(data, pos)
        wide = le & 1
        length = le >> 1
        str_start = pos
        s = data[pos:pos + length]
        pos += length
        if s == placeholder:
            return len_start, str_start, length, wide
    raise ValueError(f"placeholder {placeholder!r} not found")


def patch_placeholder(data: bytes, old: bytes, new: bytes) -> bytes:
    len_start, str_start, old_len, wide = find_placeholder(data, old)
    if wide:
        raise ValueError("wide-char placeholder is not supported by this simple patcher")
    new_bytes = new if isinstance(new, bytes) else new.encode("utf-8")
    new_le = (len(new_bytes) << 1) | 0
    return data[:len_start] + write_leb(new_le) + new_bytes + data[str_start + old_len:]


def main():
    if len(sys.argv) != 4:
        print(__doc__)
        sys.exit(1)
    src, dst, new_input = sys.argv[1], sys.argv[2], sys.argv[3]
    data = Path(src).read_bytes()
    patched = patch_placeholder(data, b"placeholder0", new_input)
    Path(dst).write_bytes(patched)
    print(f"patched {src} -> {dst} ({len(data)} -> {len(patched)} bytes)")


if __name__ == "__main__":
    main()
