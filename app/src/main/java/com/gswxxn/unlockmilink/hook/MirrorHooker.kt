package com.gswxxn.unlockmilink.hook

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ScrollView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.type.android.BundleClass
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MirrorHooker(private val deviceType : Int = 0) : YukiBaseHooker() {
    override fun onHook() {

        "$packageName.display.DisplayManagerImpl".hook {
            injectMember {
                method {
                    name = "openDisplay"
                    paramCount(3)
                }
                beforeHook {
                    field { name = "MAX_SCREEN_COUNT" }.get().set(999)
                }
            }
        }

        "$packageName.utils.DeviceUtils".hook {
            injectMember {
                method {
                    name = "isPadDevice"
                    emptyParam()
                }
                beforeHook {
                    when (deviceType) {
                        1 -> result = false
                        2 -> result = true
                    }
                }
            }
        }

        "$packageName.utils.SystemUtils".hook {
            injectMember {
                method {
                    name = "isModelSupport"
                    param(ContextClass)
                }
                replaceToTrue()
            }
        }

        "$packageName.sinkpc.PcAppendView".hook {
            injectMember {
                method {
                    name = "init"
                    param(ContextClass)
                }
                beforeHook {
                    val scrollView = ScrollView(appContext)
                    instance<FrameLayout>().addView(scrollView)
                    LayoutInflater.from(args(0).cast<Context>()).inflate(appResources.getIdentifier("pc_append_view", "layout", packageName), scrollView)
                }
                afterHook {
                    instance<FrameLayout>().removeViewAt(1)
                }
            }
        }

        "$packageName.settings.micloud.MiCloudUtils".hook {
            injectMember {
                method {
                    name = "isSupportSubScreen"
                    paramCount(1)
                }
                replaceToTrue()
            }
            injectMember {
                method {
                    name = "isSupportSubScreenOld"
                    paramCount(1)
                }
                replaceToTrue()
            }
        }
    }

    fun onXPEvent(lpparam : XC_LoadPackage.LoadPackageParam) {
        val versionCode = XposedHelpers.getStaticIntField(XposedHelpers.findClass("${lpparam.packageName}.BuildConfig", lpparam.classLoader), "VERSION_CODE")
        if (versionCode >= 30726) return
        XposedHelpers.findAndHookMethod("${lpparam.packageName}.activity.ScanQRCodeActivity", lpparam.classLoader, "onCreate", BundleClass, ChangeDeviceTypeHook(lpparam))
        XposedHelpers.findAndHookMethod("${lpparam.packageName}.connection.idm.IDMManager", lpparam.classLoader, "initIDMServer", ChangeDeviceTypeHook(lpparam))
        XposedHelpers.findAndHookMethod("${lpparam.packageName}.connection.idm.IDMManager", lpparam.classLoader, "reRegisterIDMServer", ChangeDeviceTypeHook(lpparam))
    }

    class ChangeDeviceTypeHook(private val lpparam : XC_LoadPackage.LoadPackageParam) : XC_MethodHook() {
        private var isPadHook : XC_MethodHook.Unhook? = null
        override fun beforeHookedMethod(param: MethodHookParam?) {
            super.beforeHookedMethod(param)
            isPadHook = XposedHelpers.findAndHookMethod("${lpparam.packageName}.TerminalImpl", lpparam.classLoader, "isPad", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    param?.result = false
                }
            })
        }
        override fun afterHookedMethod(param: MethodHookParam?) {
            super.afterHookedMethod(param)
            isPadHook?.unhook()
        }
    }
}