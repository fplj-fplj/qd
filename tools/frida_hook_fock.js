/*
 * Frida script for Qidian Fock sign exploration.
 *
 * Use on a rooted device/emulator with Frida server running:
 *   frida -U -f com.qidian.QDReader -l tools/frida_hook_fock.js
 *
 * This hooks high-level Java sign methods first. If you later want to hook
 * the native i_0x8756 / i_0x5353, use the offsets from analysis.md:
 *   libfockrt.so base + 0x23044 / 0x23274 (arm64-v8a)
 */

Java.perform(function () {
    function tryHook(className, methodName, overloads) {
        try {
            var cls = Java.use(className);
            overloads.forEach(function (sig) {
                cls[methodName].overload.apply(cls[methodName], sig).implementation = function () {
                    var args = Array.prototype.slice.call(arguments);
                    var ret = this[methodName].apply(this, args);
                    console.log('[Fock] ' + className + '.' + methodName + '(' + args.join(', ') + ') => ' + ret);
                    return ret;
                };
            });
            console.log('[Fock] hooked ' + className + '.' + methodName);
        } catch (e) {
            console.log('[Fock] skip ' + className + '.' + methodName + ': ' + e);
        }
    }

    tryHook('com.yuewen.fock.Fock', 'sign', [['java.lang.String']]);
    tryHook('com.qidian.QDReader.component.util.FockUtil', 'getH', [
        ['java.lang.String', 'java.lang.String', 'okhttp3.RequestBody']
    ]);
    tryHook('com.qidian.QDReader.component.util.FockUtil', 'getEncrypt', [
        ['java.lang.String', 'java.lang.String']
    ]);
    tryHook('com.qidian.QDReader.component.util.FockUtil', 'getQDInfoEncrypt', []);
    tryHook('com.yuewen.fockrt.FockRT', 'sn', [['java.lang.String']]);
});

if (Process.arch === 'arm64') {
    var lib = Module.findBaseAddress('libfockrt.so');
    if (lib) {
        console.log('[Fock] libfockrt base = ' + lib);
        // Uncomment after confirming offsets on the target build:
        // var i_8756 = lib.add(0x23044);
        // var i_5353 = lib.add(0x23274);
        // Interceptor.attach(i_8756, {
        //     onEnter: function (args) { console.log('[Fock] i_0x8756 enter'); },
        //     onLeave: function (retval) { console.log('[Fock] i_0x8756 leave ' + retval); }
        // });
    } else {
        console.log('[Fock] libfockrt.so not loaded yet');
    }
}
