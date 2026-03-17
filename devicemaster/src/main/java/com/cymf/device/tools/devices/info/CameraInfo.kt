package com.cymf.device.tools.devices.info

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Range
import android.util.Rational
import android.util.Size
import android.util.SizeF
import androidx.core.util.Pair
import com.cymf.device.R 
import com.cymf.device.tools.devices.utils.DecimalUtils
import org.json.JSONArray
import kotlin.math.atan
import kotlin.math.sqrt

/**
 * 相机信息
 */
object CameraInfo {
    fun getCameraInfo(context: Context): MutableList<Pair<String?, String?>?> {
        val list: MutableList<Pair<String?, String?>?> = ArrayList<Pair<String?, String?>?>()
        try {
            val manager = context.getSystemService(CameraManager::class.java)
            if (manager == null) {
                return list
            }
            val cameraIdList = manager.getCameraIdList()
            for (cameraId in cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // TODO 摄像头位置 后置 前置 外置
                val facing = characteristics.get<Int?>(CameraCharacteristics.LENS_FACING)
                list.add(
                    Pair<String?, String?>(
                        context.getString(R.string.menu_camera),
                        facing.toString() + " - " + getFacing(facing)
                    )
                )

                // TODO 分辨率
                val activeArraySize =
                    characteristics.get<Rect?>(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                if (activeArraySize != null) {
                    val width = activeArraySize.right - activeArraySize.left
                    val height = activeArraySize.bottom - activeArraySize.top
                    val round = DecimalUtils.round(width * height / 1000000.0, 1)
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_pixel_size),
                            round.toString() + " MP (" + width + "x" + height + ")"
                        )
                    )
                }

                //TODO 此相机设备支持的光圈大小值列表
                val lensInfoAvailableApertures =
                    characteristics.get<FloatArray?>(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                if (lensInfoAvailableApertures != null && lensInfoAvailableApertures.size != 0) {
                    val sb = StringBuilder()
                    for (f in lensInfoAvailableApertures) {
                        sb.append("f/").append(f).append(" ")
                    }
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_aperture),
                            sb.toString().trim { it <= ' ' })
                    )
                }

                //TODO 此相机设备支持的焦距列表
                val lensInfoAvailableFocalLengths =
                    characteristics.get<FloatArray?>(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                if (lensInfoAvailableFocalLengths != null && lensInfoAvailableFocalLengths.size != 0) {
                    val sb = StringBuilder()
                    for (f in lensInfoAvailableFocalLengths) {
                        sb.append(f).append(" mm").append(" ")
                    }
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_focal_length),
                            sb.toString().trim { it <= ' ' })
                    )
                }

                // TODO 相机设备支持的自动对焦（AF）模式列表
                val afAvailableModes =
                    characteristics.get<IntArray?>(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                if (afAvailableModes != null && afAvailableModes.size != 0) {
                    val sb = StringBuilder()
                    for (i in afAvailableModes) {
                        sb.append(getAfAvailableModes(i)).append(",")
                    }
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_af_modes),
                            sb.deleteCharAt(sb.length - 1).toString()
                        )
                    )
                }

                // TODO 传感器尺寸
                val physicalSize =
                    characteristics.get<SizeF?>(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                if (physicalSize != null) {
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_size),
                            physicalSize.getWidth().toString() + "x" + physicalSize.getHeight()
                        )
                    )
                }

                // TODO 像素大小
                if (physicalSize != null && activeArraySize != null) {
                    val value = (physicalSize.getWidth() * physicalSize.getHeight()).toDouble()
                    val width = (activeArraySize.right - activeArraySize.left).toDouble()
                    val height = (activeArraySize.bottom - activeArraySize.top).toDouble()
                    val round =
                        DecimalUtils.round(sqrt((((value * 1000.0) / width) * 1000.0) / height), 2)
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_pixel_size),
                            "~" + round + " µm"
                        )
                    )
                }

                // TODO 视角
                val focalLengths =
                    characteristics.get<FloatArray?>(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                if (focalLengths != null && focalLengths.size > 0) {
                    val f = focalLengths[0]
                    val sizeF =
                        characteristics.get<SizeF?>(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    if (sizeF != null) {
                        val width = sizeF.getWidth()
                        if (width > 0.0f) {
                            list.add(
                                Pair<String?, String?>(
                                    context.getString(R.string.camera_view_angle),
                                    DecimalUtils.round(
                                        Math.toDegrees(atan((width * 0.5) / f)) * 2.0,
                                        1
                                    ).toString() + "°"
                                )
                            )
                        }
                    }
                }

                // TODO 摄像头支持图像格式
                val map =
                    characteristics.get<StreamConfigurationMap?>(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                if (map != null) {
                    val ints = map.getOutputFormats()
                    val sb = StringBuffer()
                    for (i in ints) {
                        sb.append(getFormat(i)).append(",")
                    }
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_formats),
                            sb.deleteCharAt(sb.length - 1).toString()
                        )
                    )
                }

                //TODO 本相机设备支持的感光度范围
                val sensorInfoSensitivityRange =
                    characteristics.get<Range<Int?>?>(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                if (sensorInfoSensitivityRange != null) {
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_iso),
                            sensorInfoSensitivityRange.getLower()
                                .toString() + "-" + sensorInfoSensitivityRange.getUpper()
                        )
                    )
                }

                //TODO 彩色滤光片在传感器上的布置
                val sensorInfoColorFilterArrangement =
                    characteristics.get<Int?>(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
                if (sensorInfoColorFilterArrangement != null) {
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_color_filter),
                            getSensorInfoColorFilterArrangement(sensorInfoColorFilterArrangement)
                        )
                    )
                }

                //TODO 需要以顺时针方向旋转输出图像以使其在设备屏幕上以其原始方向直立
                val sensorOrientation =
                    characteristics.get<Int?>(CameraCharacteristics.SENSOR_ORIENTATION)
                if (sensorOrientation != null) {
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_orientation),
                            sensorOrientation.toString()
                        )
                    )
                }

                // TODO 摄像头是否支持闪光灯
                val flashInfoAvailable =
                    characteristics.get<Boolean?>(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                if (flashInfoAvailable != null) {
                    list.add(
                        Pair<String?, String?>(
                            context.getString(R.string.camera_flash),
                            flashInfoAvailable.toString()
                        )
                    )
                }

                // ==================================================================================================

                //TODO 通常对相机设备功能的总体分类
                list.add(
                    Pair<String?, String?>(
                        "SupportedHardwareLevel",
                        getLevel(characteristics.get<Int?>(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL))
                    )
                )

                //TODO 本相机设备支持的像差校正模式列表
                val aberrationModes =
                    characteristics.get<IntArray?>(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES)
                if (aberrationModes != null && aberrationModes.size != 0) {
                    val jsonArrayAberrationModes = JSONArray()
                    for (i in aberrationModes) {
                        jsonArrayAberrationModes.put(getAberrationModes(i))
                    }
                    list.add(
                        Pair<String?, String?>(
                            "AberrationModes",
                            jsonArrayAberrationModes.toString()
                        )
                    )
                }
                //TODO 本相机设备支持的自动曝光防条纹模式列表
                val antiBandingModes =
                    characteristics.get<IntArray?>(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES)
                if (antiBandingModes != null && antiBandingModes.size != 0) {
                    val jsonArrayAntiBandingModes = JSONArray()
                    for (i in antiBandingModes) {
                        jsonArrayAntiBandingModes.put(getAntiBandingModes(i))
                    }
                    list.add(
                        Pair<String?, String?>(
                            "AntiBandingModes",
                            jsonArrayAntiBandingModes.toString()
                        )
                    )
                }
                //TODO 本相机设备支持的自动曝光模式列表
                if (CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES != null) {
                    val aeAvailableModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
                    if (aeAvailableModes != null && aeAvailableModes.size != 0) {
                        val jsonArrayAeAvailableModes = JSONArray()
                        for (i in aeAvailableModes) {
                            jsonArrayAeAvailableModes.put(getAeAvailableModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AeAvailableModes",
                                jsonArrayAeAvailableModes.toString()
                            )
                        )
                    }
                }
                //TODO 此相机设备支持的最大和最小曝光补偿值
                if (CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE != null) {
                    val compensationRange =
                        characteristics.get<Range<Int?>?>(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                    if (compensationRange != null) {
                        list.add(
                            Pair<String?, String?>(
                                "CompensationRange",
                                compensationRange.toString()
                            )
                        )
                    }
                }
                //TODO 可以更改曝光补偿的最小步长
                if (CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP != null) {
                    val compensationStep =
                        characteristics.get<Rational?>(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                    if (compensationStep != null) {
                        list.add(
                            Pair<String?, String?>(
                                "CompensationStep",
                                compensationStep.toDouble().toString() + ""
                            )
                        )
                    }
                }
                //TODO 是否锁定自动曝光
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE != null) {
                    val lockAvailable =
                        characteristics.get<Boolean?>(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE)
                    if (lockAvailable != null) {
                        list.add(
                            Pair<String?, String?>(
                                "LockAvailable",
                                lockAvailable.toString() + ""
                            )
                        )
                    }
                }

                //TODO 本相机设备支持的色彩效果列表
                if (CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS != null) {
                    val availableEffects =
                        characteristics.get<IntArray?>(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
                    if (availableEffects != null && availableEffects.size != 0) {
                        val jsonArrayAvailableEffects = JSONArray()
                        for (i in availableEffects) {
                            jsonArrayAvailableEffects.put(getAvailableEffects(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableEffects",
                                jsonArrayAvailableEffects.toString()
                            )
                        )
                    }
                }
                //TODO 本相机设备支持的控制模式列表
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.CONTROL_AVAILABLE_MODES != null) {
                    val availableModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.CONTROL_AVAILABLE_MODES)
                    if (availableModes != null && availableModes.size != 0) {
                        val jsonArrayAvailableModes = JSONArray()
                        for (i in availableModes) {
                            jsonArrayAvailableModes.put(getAvailableModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableModes",
                                jsonArrayAvailableModes.toString()
                            )
                        )
                    }
                }
                //TODO 本相机设备支持的场景模式列表
                if (CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES != null) {
                    val availableSceneModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
                    if (availableSceneModes != null && availableSceneModes.size != 0) {
                        val jsonArrayAvailableSceneModes = JSONArray()
                        for (i in availableSceneModes) {
                            jsonArrayAvailableSceneModes.put(getAvailableSceneModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableSceneModes",
                                jsonArrayAvailableSceneModes.toString()
                            )
                        )
                    }
                }
                //TODO 本相机设备支持的视频稳定模式列表
                if (CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES != null) {
                    val videoStabilizationModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                    if (videoStabilizationModes != null && videoStabilizationModes.size != 0) {
                        val jsonArrayVideoStabilizationModes = JSONArray()
                        for (i in videoStabilizationModes) {
                            jsonArrayVideoStabilizationModes.put(getVideoStabilizationModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "VideoStabilizationModes",
                                jsonArrayVideoStabilizationModes.toString()
                            )
                        )
                    }
                }
                //TODO 本相机设备支持的自动白平衡模式列表
                if (CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES != null) {
                    val awbAvailableModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
                    if (awbAvailableModes != null && awbAvailableModes.size != 0) {
                        val jsonArrayAwbAvailableModes = JSONArray()
                        for (i in awbAvailableModes) {
                            jsonArrayAwbAvailableModes.put(getAwbAvailableModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AwbAvailableModes",
                                jsonArrayAwbAvailableModes.toString()
                            )
                        )
                    }
                }

                //TODO 设备是否支持自动白平衡
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE != null) {
                    val awbLockAvailable =
                        characteristics.get<Boolean?>(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE)
                    if (awbLockAvailable != null) {
                        list.add(
                            Pair<String?, String?>(
                                "AwbLockAvailable",
                                awbLockAvailable.toString() + ""
                            )
                        )
                    }
                }

                //TODO 自动曝光（AE）例程可以使用的最大测光区域数
                if (CameraCharacteristics.CONTROL_MAX_REGIONS_AE != null) {
                    val maxRegionsAe: Int =
                        characteristics.get<Int?>(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)!!
                    list.add(Pair<String?, String?>("MaxRegionsAe", maxRegionsAe.toString() + ""))
                }

                //TODO 自动对焦（AF）例程可以使用的最大测光区域数
                if (CameraCharacteristics.CONTROL_MAX_REGIONS_AF != null) {
                    val maxRegionsAf: Int =
                        characteristics.get<Int?>(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)!!
                    list.add(Pair<String?, String?>("MaxRegionsAf", maxRegionsAf.toString() + ""))
                }

                //TODO 自动白平衡（AWB）例程可以使用的最大测光区域数
                if (CameraCharacteristics.CONTROL_MAX_REGIONS_AWB != null) {
                    val maxRegionsAwb: Int =
                        characteristics.get<Int?>(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)!!
                    list.add(Pair<String?, String?>("MaxRegionsAwb", maxRegionsAwb.toString() + ""))
                }

                //TODO 相机设备支持的增强范围
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && CameraCharacteristics.CONTROL_POST_RAW_SENSITIVITY_BOOST_RANGE != null) {
                    val rawSensitivityBoostRange =
                        characteristics.get<Range<Int?>?>(CameraCharacteristics.CONTROL_POST_RAW_SENSITIVITY_BOOST_RANGE)
                    if (rawSensitivityBoostRange != null) {
                        list.add(
                            Pair<String?, String?>(
                                "RawSensitivityBoostRange",
                                rawSensitivityBoostRange.toString()
                            )
                        )
                    }
                }

                //TODO 指示捕获请求是否可以同时针对DEPTH16 / DEPTH_POINT_CLOUD输出和常规彩色输出 true为不可以
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.DEPTH_DEPTH_IS_EXCLUSIVE != null) {
                    val depthIsExclusive =
                        characteristics.get<Boolean?>(CameraCharacteristics.DEPTH_DEPTH_IS_EXCLUSIVE)
                    if (depthIsExclusive != null) {
                        list.add(
                            Pair<String?, String?>(
                                "DepthIsExclusive",
                                depthIsExclusive.toString() + ""
                            )
                        )
                    }
                }


                //TODO 相机设备支持的帧频范围列表
                if (CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES != null) {
                    val fpsRanges =
                        characteristics.get<Array<Range<Int?>?>?>(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    if (fpsRanges != null && fpsRanges.size != 0) {
                        val jsonArrayFpsRanges = JSONArray()
                        for (i in fpsRanges) {
                            jsonArrayFpsRanges.put(i)
                        }
                        list.add(Pair<String?, String?>("FpsRanges", jsonArrayFpsRanges.toString()))
                    }
                }

                //TODO 本相机设备支持的失真校正模式列表
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES != null) {
                    val correctionAvailableModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES)
                    if (correctionAvailableModes != null && correctionAvailableModes.size != 0) {
                        val jsonArrayCorrectionAvailableModes = JSONArray()
                        for (i in correctionAvailableModes) {
                            jsonArrayCorrectionAvailableModes.put(getCorrectionAvailableModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "CorrectionAvailableModes",
                                jsonArrayCorrectionAvailableModes.toString()
                            )
                        )
                    }
                }


                //TODO 本相机设备支持的边缘增强模式列表
                if (CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES != null) {
                    val availableEdgeModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)
                    if (availableEdgeModes != null && availableEdgeModes.size != 0) {
                        val jsonArrayAvailableEdgeModes = JSONArray()
                        for (i in availableEdgeModes) {
                            jsonArrayAvailableEdgeModes.put(getAvailableEdgeModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableEdgeModes",
                                jsonArrayAvailableEdgeModes.toString()
                            )
                        )
                    }
                }

                //TODO 本相机设备支持的热像素校正模式列表
                if (CameraCharacteristics.HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES != null) {
                    val availableHotPixelModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES)
                    if (availableHotPixelModes != null && availableHotPixelModes.size != 0) {
                        val jsonArrayAvailableHotPixelModes = JSONArray()
                        for (i in availableHotPixelModes) {
                            jsonArrayAvailableHotPixelModes.put(getAvailableHotPixelModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableHotPixelModes",
                                jsonArrayAvailableHotPixelModes.toString()
                            )
                        )
                    }
                }

                //TODO 摄像机设备制造商版本信息的简短字符串
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && CameraCharacteristics.INFO_VERSION != null) {
                    list.add(
                        Pair<String?, String?>(
                            "InfoVersion",
                            characteristics.get<String?>(CameraCharacteristics.INFO_VERSION)
                        )
                    )
                }
                //TODO 此相机设备支持的JPEG缩略图尺寸列表
                if (CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES != null) {
                    val jpegAvailableThumbnailSizes: Array<Size>? =
                        characteristics.get<Array<Size?>?>(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES) as Array<Size>?
                    val jsonArrayJpegAvailableThumbnailSizes = JSONArray()
                    if (jpegAvailableThumbnailSizes != null && jpegAvailableThumbnailSizes.size != 0) {
                        for (s in jpegAvailableThumbnailSizes) {
                            jsonArrayJpegAvailableThumbnailSizes.put(s.toString())
                        }
                    }
                    list.add(
                        Pair<String?, String?>(
                            "JpegAvailableThumbnailSizes",
                            jsonArrayJpegAvailableThumbnailSizes.toString()
                        )
                    )
                }
                //TODO 用于校正此相机设备的径向和切向镜头失真的校正系数
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && CameraCharacteristics.LENS_DISTORTION != null) {
                    val lensDistortion =
                        characteristics.get<FloatArray?>(CameraCharacteristics.LENS_DISTORTION)
                    if (lensDistortion != null && lensDistortion.size != 0) {
                        list.add(
                            Pair<String?, String?>(
                                "LensDistortion",
                                JSONArray(lensDistortion).toString()
                            )
                        )
                    }
                }
                //TODO 此相机设备支持的中性密度滤镜值列表
                if (CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES != null) {
                    val lensInfoAvailableFilterDensities =
                        characteristics.get<FloatArray?>(CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES)
                    if (lensInfoAvailableFilterDensities != null && lensInfoAvailableFilterDensities.size != 0) {
                        list.add(
                            Pair<String?, String?>(
                                "LensInfoAvailableFilterDensities",
                                JSONArray(lensInfoAvailableFilterDensities).toString()
                            )
                        )
                    }

                    //TODO 本相机设备支持的光学防抖（OIS）模式列表
                    val availableOpticalStabilization =
                        characteristics.get<IntArray?>(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                    if (availableOpticalStabilization != null && availableOpticalStabilization.size != 0) {
                        val jsonArrayAvailableOpticalStabilization = JSONArray()
                        for (i in availableOpticalStabilization) {
                            jsonArrayAvailableOpticalStabilization.put(
                                getAvailableOpticalStabilization(i)
                            )
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableOpticalStabilization",
                                jsonArrayAvailableOpticalStabilization.toString()
                            )
                        )
                    }
                }
                //TODO 镜头焦距校准质量
                if (CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION != null) {
                    val focusDistanceCalibration =
                        characteristics.get<Int?>(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION)
                    list.add(
                        Pair<String?, String?>(
                            "FocusDistanceCalibration",
                            getFocusDistanceCalibration(focusDistanceCalibration).toString()
                        )
                    )
                }
                //TODO 镜头的超焦距
                val hyperFocalDistance: Float =
                    characteristics.get<Float?>(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)!!
                list.add(
                    Pair<String?, String?>(
                        "HyperFocalDistance",
                        hyperFocalDistance.toString() + ""
                    )
                )
                //TODO 距镜头最前面的最短距离，可使其聚焦
                if (CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE != null) {
                    val minimumFocusDistance: Float =
                        characteristics.get<Float?>(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!
                    list.add(
                        Pair<String?, String?>(
                            "MinimumFocusDistance",
                            minimumFocusDistance.toString() + ""
                        )
                    )
                }
                //TODO 本相机设备固有校准的参数
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.LENS_INTRINSIC_CALIBRATION != null) {
                    val lensIntrinsicCalibration =
                        characteristics.get<FloatArray?>(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
                    if (lensIntrinsicCalibration != null && lensIntrinsicCalibration.size != 0) {
                        list.add(
                            Pair<String?, String?>(
                                "LensIntrinsicCalibration",
                                JSONArray(lensIntrinsicCalibration).toString()
                            )
                        )
                    }
                }
                //TODO 镜头姿势
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && CameraCharacteristics.LENS_POSE_REFERENCE != null) {
                    val lensPoseReference =
                        characteristics.get<Int?>(CameraCharacteristics.LENS_POSE_REFERENCE)
                    list.add(
                        Pair<String?, String?>(
                            "LensPoseReference",
                            getLensPoseReference(lensPoseReference)
                        )
                    )
                }
                //TODO 相机相对于传感器坐标系的方向
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.LENS_POSE_ROTATION != null) {
                    val lensPoseRotation =
                        characteristics.get<FloatArray?>(CameraCharacteristics.LENS_POSE_ROTATION)
                    if (lensPoseRotation != null && lensPoseRotation.size != 0) {
                        list.add(
                            Pair<String?, String?>(
                                "LensPoseRotation",
                                JSONArray(lensPoseRotation).toString()
                            )
                        )
                    }
                }
                //TODO 相机光学中心的位置
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.LENS_POSE_TRANSLATION != null) {
                    val lensPoseTranslation =
                        characteristics.get<FloatArray?>(CameraCharacteristics.LENS_POSE_TRANSLATION)
                    if (lensPoseTranslation != null && lensPoseTranslation.size != 0) {
                        list.add(
                            Pair<String?, String?>(
                                "LensPoseTranslation",
                                JSONArray(lensPoseTranslation).toString()
                            )
                        )
                    }
                }
                //TODO 帧时间戳同步
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && CameraCharacteristics.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE != null) {
                    val cameraSensorSyncType =
                        characteristics.get<Int?>(CameraCharacteristics.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE)
                    list.add(
                        Pair<String?, String?>(
                            "CameraSensorSyncType",
                            getCameraSensorSyncType(cameraSensorSyncType)
                        )
                    )
                }
                //TODO 本相机设备支持的降噪模式列表
                if (CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES != null) {
                    val availableNoiseReductionModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)
                    if (availableNoiseReductionModes != null && availableNoiseReductionModes.size != 0) {
                        val jsonArrayAvailableNoiseReductionModes = JSONArray()
                        for (i in availableNoiseReductionModes) {
                            jsonArrayAvailableNoiseReductionModes.put(
                                getAvailableNoiseReductionModes(i)
                            )
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableNoiseReductionModes",
                                jsonArrayAvailableNoiseReductionModes.toString()
                            )
                        )
                    }
                }
                //TODO 最大摄像机捕获流水线停顿
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.REPROCESS_MAX_CAPTURE_STALL != null) {
                    val maxCaptureStall =
                        characteristics.get<Int?>(CameraCharacteristics.REPROCESS_MAX_CAPTURE_STALL)
                    if (maxCaptureStall != null) {
                        list.add(
                            Pair<String?, String?>(
                                "MaxCaptureStall",
                                maxCaptureStall.toString()
                            )
                        )
                    }
                }
                //TODO 此相机设备宣传为完全支持的功能列表
                if (CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES != null) {
                    val requestAvailableCapabilities =
                        characteristics.get<IntArray?>(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    if (requestAvailableCapabilities != null && requestAvailableCapabilities.size != 0) {
                        val jsonArrayRequestAvailableCapabilities = JSONArray()
                        for (i in requestAvailableCapabilities) {
                            jsonArrayRequestAvailableCapabilities.put(
                                getRequestAvailableCapabilities(i)
                            )
                        }
                        list.add(
                            Pair<String?, String?>(
                                "RequestAvailableCapabilities",
                                jsonArrayRequestAvailableCapabilities.toString()
                            )
                        )
                    }
                }

                //TODO 摄像机设备可以同时配置和使用的任何类型的输入流的最大数量
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.REQUEST_MAX_NUM_INPUT_STREAMS != null) {
                    val requestMaxNumInputStreams =
                        characteristics.get<Int?>(CameraCharacteristics.REQUEST_MAX_NUM_INPUT_STREAMS)
                    if (requestMaxNumInputStreams != null) {
                        list.add(
                            Pair<String?, String?>(
                                "RequestMaxNumInputStreams",
                                requestMaxNumInputStreams.toString()
                            )
                        )
                    }
                }

                //TODO 相机设备可以针对任何已处理（但不是陈旧）格式同时配置和使用的不同类型的输出流的最大数量
                if (CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC != null) {
                    val requestMaxNumOutputProc =
                        characteristics.get<Int?>(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC)
                    if (requestMaxNumOutputProc != null) {
                        list.add(
                            Pair<String?, String?>(
                                "RequestMaxNumOutputProc",
                                requestMaxNumOutputProc.toString()
                            )
                        )
                    }
                }

                //TODO 相机设备可以针对任何已处理（和停顿）格式同时配置和使用的不同类型的输出流的最大数量
                if (CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC_STALLING != null) {
                    val requestMaxNumOutputProcStalling =
                        characteristics.get<Int?>(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC_STALLING)
                    if (requestMaxNumOutputProcStalling != null) {
                        list.add(
                            Pair<String?, String?>(
                                "RequestMaxNumOutputProcStalling",
                                requestMaxNumOutputProcStalling.toString()
                            )
                        )
                    }
                }

                //TODO 相机设备可以针对任何RAW格式同时配置和使用的不同类型的输出流的最大数量
                if (CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_RAW != null) {
                    val requestMaxNumOutputRaw =
                        characteristics.get<Int?>(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_RAW)
                    if (requestMaxNumOutputRaw != null) {
                        list.add(
                            Pair<String?, String?>(
                                "RequestMaxNumOutputRaw",
                                requestMaxNumOutputRaw.toString()
                            )
                        )
                    }
                }

                //TODO 定义结果将由多少个子组件组成
                if (CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT != null) {
                    val requestPartialResultCount =
                        characteristics.get<Int?>(CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT)
                    if (requestPartialResultCount != null) {
                        list.add(
                            Pair<String?, String?>(
                                "RequestPartialResultCount",
                                requestPartialResultCount.toString()
                            )
                        )
                    }
                }

                //TODO 指定从暴露帧到框架可用时必须经历的最大管道阶段数
                if (CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH != null) {
                    val requestPipelineMaxDepth =
                        characteristics.get<Byte?>(CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH)
                    if (requestPipelineMaxDepth != null) {
                        list.add(
                            Pair<String?, String?>(
                                "RequestPipelineMaxDepth",
                                requestPipelineMaxDepth.toString()
                            )
                        )
                    }
                }

                //TODO 活动区域宽度和作物区域宽度以及活动区域高度和作物区域高度之间的最大比率
                if (CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM != null) {
                    val scalerAvailableMaxDigitalZoom =
                        characteristics.get<Float?>(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
                    if (scalerAvailableMaxDigitalZoom != null) {
                        list.add(
                            Pair<String?, String?>(
                                "ScalerAvailableMaxDigitalZoom",
                                scalerAvailableMaxDigitalZoom.toString()
                            )
                        )
                    }
                }

                //TODO 该相机设备支持的裁切类型
                if (CameraCharacteristics.SCALER_CROPPING_TYPE != null) {
                    val scalerCroppingType =
                        characteristics.get<Int?>(CameraCharacteristics.SCALER_CROPPING_TYPE)
                    if (scalerCroppingType != null) {
                        list.add(
                            Pair<String?, String?>(
                                "ScalerCroppingType",
                                getScalerCroppingType(scalerCroppingType)
                            )
                        )
                    }
                }

                //TODO  此相机设备支持的传感器测试图案模式列表
                if (CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES != null) {
                    val sensorAvailableTestPatternModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES)
                    if (sensorAvailableTestPatternModes != null && sensorAvailableTestPatternModes.size != 0) {
                        val jsonArraySensorAvailableTestPatternModes = JSONArray()
                        for (i in sensorAvailableTestPatternModes) {
                            jsonArraySensorAvailableTestPatternModes.put(
                                getSensorAvailableTestPatternModes(i)
                            )
                        }
                        list.add(
                            Pair<String?, String?>(
                                "SensorAvailableTestPatternModes",
                                jsonArraySensorAvailableTestPatternModes.toString()
                            )
                        )
                    }
                }

                //TODO 此相机设备支持的图像曝光时间范围
                if (CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE != null) {
                    val sensorInfoExposureTimeRange =
                        characteristics.get<Range<Long?>?>(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                    if (sensorInfoExposureTimeRange != null) {
                        list.add(
                            Pair<String?, String?>(
                                "SensorInfoExposureTimeRange",
                                sensorInfoExposureTimeRange.toString()
                            )
                        )
                    }
                }

                //TODO 从本相机设备输出的RAW图像是否经过镜头阴影校正
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.SENSOR_INFO_LENS_SHADING_APPLIED != null) {
                    val sensorInfoLensShadingApplied =
                        characteristics.get<Boolean?>(CameraCharacteristics.SENSOR_INFO_LENS_SHADING_APPLIED)
                    if (sensorInfoLensShadingApplied != null) {
                        list.add(
                            Pair<String?, String?>(
                                "SensorInfoLensShadingApplied",
                                sensorInfoLensShadingApplied.toString()
                            )
                        )
                    }
                }

                //TODO 本相机设备支持的最大可能帧时长
                if (CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION != null) {
                    val sensorInfoaxFrameDuration =
                        characteristics.get<Long?>(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION)
                    if (sensorInfoaxFrameDuration != null) {
                        list.add(
                            Pair<String?, String?>(
                                "SensorInfoaxFrameDuration",
                                sensorInfoaxFrameDuration.toString()
                            )
                        )
                    }
                }

                //TODO 传感器捕获开始时间戳记的时基源
                if (CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE != null) {
                    val sensorInfoTimestampSource =
                        characteristics.get<Int?>(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE)
                    if (sensorInfoTimestampSource != null) {
                        list.add(
                            Pair<String?, String?>(
                                "SensorInfoTimestampSource",
                                getSensorInfoTimestampSource(sensorInfoTimestampSource)
                            )
                        )
                    }
                }

                //TODO 传感器输出的最大原始值
                if (CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL != null) {
                    val sensorInfoWhiteLevel =
                        characteristics.get<Int?>(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL)
                    if (sensorInfoWhiteLevel != null) {
                        list.add(
                            Pair<String?, String?>(
                                "SensorInfoWhiteLevel",
                                sensorInfoWhiteLevel.toString()
                            )
                        )
                    }
                }

                //TODO 纯粹通过模拟增益实现的最大灵敏度
                if (CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY != null) {
                    val sensorMaxAnalogSensitivity =
                        characteristics.get<Int?>(CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY)
                    if (sensorMaxAnalogSensitivity != null) {
                        list.add(
                            Pair<String?, String?>(
                                "SensorMaxAnalogSensitivity",
                                sensorMaxAnalogSensitivity.toString()
                            )
                        )
                    }
                }

                //TODO 用作场景光源的标准参考光源
                if (CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1 != null) {
                    val sensorReferenceIlluminant1 =
                        characteristics.get<Int?>(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1)
                    if (sensorReferenceIlluminant1 != null) {
                        list.add(
                            Pair<String?, String?>(
                                "SensorReferenceIlluminant1",
                                getSensorReferenceIlluminant1(sensorReferenceIlluminant1)
                            )
                        )
                    }
                }

                //TODO 本相机设备支持的镜头阴影模式列表
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.SHADING_AVAILABLE_MODES != null) {
                    val shadingAvailableModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.SHADING_AVAILABLE_MODES)
                    if (shadingAvailableModes != null && shadingAvailableModes.size != 0) {
                        val jsonArrayShadingAvailableModes = JSONArray()
                        for (i in shadingAvailableModes) {
                            jsonArrayShadingAvailableModes.put(getShadingAvailableModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "ShadingAvailableModes",
                                jsonArrayShadingAvailableModes.toString()
                            )
                        )
                    }
                }

                //TODO 本相机设备支持的脸部识别模式列表
                if (CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES != null) {
                    val availableFaceDetectModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)
                    if (availableFaceDetectModes != null && availableFaceDetectModes.size != 0) {
                        val jsonArrayAvailableFaceDetectModes = JSONArray()
                        for (i in availableFaceDetectModes) {
                            jsonArrayAvailableFaceDetectModes.put(getAvailableFaceDetectModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableFaceDetectModes",
                                jsonArrayAvailableFaceDetectModes.toString()
                            )
                        )
                    }
                }

                //TODO 本相机设备支持的镜头阴影贴图输出模式列表
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES != null) {
                    val availableLensShadingMapModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES)
                    if (availableLensShadingMapModes != null && availableLensShadingMapModes.size != 0) {
                        val jsonArrayAvailableLensShadingMapModes = JSONArray()
                        for (i in availableLensShadingMapModes) {
                            jsonArrayAvailableLensShadingMapModes.put(
                                getAvailableLensShadingMapModes(i)
                            )
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableLensShadingMapModes",
                                jsonArrayAvailableLensShadingMapModes.toString()
                            )
                        )
                    }
                }

                //TODO 本相机设备支持的OIS数据输出模式列表
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && CameraCharacteristics.STATISTICS_INFO_AVAILABLE_OIS_DATA_MODES != null) {
                    val availableOisDataModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_OIS_DATA_MODES)
                    if (availableOisDataModes != null && availableOisDataModes.size != 0) {
                        val jsonArrayAvailableOisDataModes = JSONArray()
                        for (i in availableOisDataModes) {
                            jsonArrayAvailableOisDataModes.put(getAvailableOisDataModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableOisDataModes",
                                jsonArrayAvailableOisDataModes.toString()
                            )
                        )
                    }
                }


                //TODO 同时可检测到的脸部的最大数量
                if (CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT != null) {
                    val statisticsInfoMaxFaceCount =
                        characteristics.get<Int?>(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT)
                    if (statisticsInfoMaxFaceCount != null) {
                        list.add(
                            Pair<String?, String?>(
                                "StatisticsInfoMaxFaceCount",
                                statisticsInfoMaxFaceCount.toString()
                            )
                        )
                    }
                }

                //TODO 提交请求后（与前一个请求不同）并且结果状态变为同步之前可以出现的最大帧数
                if (CameraCharacteristics.SYNC_MAX_LATENCY != null) {
                    val syncMaxLatency =
                        characteristics.get<Int?>(CameraCharacteristics.SYNC_MAX_LATENCY)
                    if (syncMaxLatency != null) {
                        list.add(
                            Pair<String?, String?>(
                                "SyncMaxLatency",
                                getSyncMaxLatency(syncMaxLatency)
                            )
                        )
                    }
                }

                //TODO 本相机设备支持的色调映射模式列表
                if (CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES != null) {
                    val availableToneMapModes =
                        characteristics.get<IntArray?>(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES)
                    if (availableToneMapModes != null && availableToneMapModes.size != 0) {
                        val jsonArrayAvailableToneMapModes = JSONArray()
                        for (i in availableToneMapModes) {
                            jsonArrayAvailableToneMapModes.put(getAvailableToneMapModes(i))
                        }
                        list.add(
                            Pair<String?, String?>(
                                "AvailableToneMapModes",
                                jsonArrayAvailableToneMapModes.toString()
                            )
                        )
                    }
                }

                //TODO 色调图曲线中可用于的最大支持点数
                if (CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS != null) {
                    val tonemapMaxCurvePoints =
                        characteristics.get<Int?>(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS)
                    if (tonemapMaxCurvePoints != null) {
                        list.add(
                            Pair<String?, String?>(
                                "TonemapMaxCurvePoints",
                                tonemapMaxCurvePoints.toString()
                            )
                        )
                    }
                }
                // TODO 另外摄像头
                list.add(Pair<String?, String?>("", ""))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun getAvailableToneMapModes(availableToneMapModes: Int): String {
        when (availableToneMapModes) {
            CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE -> return "CONTRAST_CURVE"
            CaptureRequest.TONEMAP_MODE_FAST -> return "FAST"
            CaptureRequest.TONEMAP_MODE_GAMMA_VALUE -> return "GAMMA_VALUE"
            CaptureRequest.TONEMAP_MODE_HIGH_QUALITY -> return "HIGH_QUALITY"
            CaptureRequest.TONEMAP_MODE_PRESET_CURVE -> return "PRESET_CURVE"
            else -> return "UNKNOWN" + "-" + availableToneMapModes

        }
    }


    private fun getSyncMaxLatency(syncMaxLatency: Int): String {
        when (syncMaxLatency) {
            CaptureRequest.SYNC_MAX_LATENCY_UNKNOWN -> return "UNKNOWN"
            CaptureRequest.SYNC_MAX_LATENCY_PER_FRAME_CONTROL -> return "PER_FRAME_CONTROL"
            else -> return "UNKNOWN" + "-" + syncMaxLatency

        }
    }

    private fun getAvailableOisDataModes(availableOisDataModes: Int): String {
        when (availableOisDataModes) {
            CaptureRequest.STATISTICS_OIS_DATA_MODE_ON -> return "ON"
            CaptureRequest.STATISTICS_OIS_DATA_MODE_OFF -> return "OFF"
            else -> return "UNKNOWN" + "-" + availableOisDataModes

        }
    }

    private fun getAvailableLensShadingMapModes(availableLensShadingMapModes: Int): String {
        when (availableLensShadingMapModes) {
            CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON -> return "ON"
            CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_OFF -> return "OFF"
            else -> return "UNKNOWN" + "-" + availableLensShadingMapModes

        }
    }

    private fun getAvailableFaceDetectModes(availableFaceDetectModes: Int): String {
        when (availableFaceDetectModes) {
            CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL -> return "FULL"
            CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE -> return "SIMPLE"
            CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF -> return "OFF"
            else -> return "UNKNOWN" + "-" + availableFaceDetectModes

        }
    }

    private fun getShadingAvailableModes(shadingAvailableModes: Int): String {
        when (shadingAvailableModes) {
            CaptureRequest.SHADING_MODE_FAST -> return "FAST"
            CaptureRequest.SHADING_MODE_HIGH_QUALITY -> return "HIGH_QUALITY"
            CaptureRequest.SHADING_MODE_OFF -> return "OFF"
            else -> return "UNKNOWN" + "-" + shadingAvailableModes

        }
    }


    private fun getSensorReferenceIlluminant1(sensorReferenceIlluminant1: Int): String {
        when (sensorReferenceIlluminant1) {
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_CLOUDY_WEATHER -> return "CLOUDY_WEATHER"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_COOL_WHITE_FLUORESCENT -> return "COOL_WHITE_FLUORESCENT"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_D50 -> return "D50"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_D55 -> return "D55"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_D65 -> return "D65"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_D75 -> return "D75"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_DAY_WHITE_FLUORESCENT -> return "DAY_WHITE_FLUORESCENT"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_DAYLIGHT -> return "DAYLIGHT"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_DAYLIGHT_FLUORESCENT -> return "DAYLIGHT_FLUORESCENT"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_FINE_WEATHER -> return "FINE_WEATHER"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_FLASH -> return "FLASH"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_FLUORESCENT -> return "FLUORESCENT"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_ISO_STUDIO_TUNGSTEN -> return "ISO_STUDIO_TUNGSTEN"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_SHADE -> return "SHADE"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_STANDARD_A -> return "STANDARD_A"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_STANDARD_B -> return "STANDARD_B"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_STANDARD_C -> return "STANDARD_C"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_TUNGSTEN -> return "TUNGSTEN"
            CaptureRequest.SENSOR_REFERENCE_ILLUMINANT1_WHITE_FLUORESCENT -> return "WHITE_FLUORESCENT"

            else -> return "UNKNOWN" + "-" + sensorReferenceIlluminant1

        }
    }

    private fun getSensorInfoTimestampSource(sensorInfoTimestampSource: Int): String {
        when (sensorInfoTimestampSource) {
            CaptureRequest.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN -> return "UNKNOWN"
            CaptureRequest.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME -> return "REALTIME"
            else -> return "UNKNOWN" + "-" + sensorInfoTimestampSource

        }
    }

    private fun getSensorInfoColorFilterArrangement(sensorInfoColorFilterArrangement: Int): String {
        when (sensorInfoColorFilterArrangement) {
            CaptureRequest.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR -> return "BGGR"
            CaptureRequest.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG -> return "GBRG"
            CaptureRequest.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG -> return "GRBG"
            CaptureRequest.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGB -> return "RGB"
            CaptureRequest.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB -> return "RGGB"
            else -> return "UNKNOWN" + "-" + sensorInfoColorFilterArrangement

        }
    }

    private fun getSensorAvailableTestPatternModes(sensorAvailableTestPatternModes: Int): String {
        when (sensorAvailableTestPatternModes) {
            CaptureRequest.SENSOR_TEST_PATTERN_MODE_COLOR_BARS -> return "COLOR_BARS"
            CaptureRequest.SENSOR_TEST_PATTERN_MODE_COLOR_BARS_FADE_TO_GRAY -> return "COLOR_BARS_FADE_TO_GRAY"
            CaptureRequest.SENSOR_TEST_PATTERN_MODE_CUSTOM1 -> return "CUSTOM1"
            CaptureRequest.SENSOR_TEST_PATTERN_MODE_OFF -> return "OFF"
            CaptureRequest.SENSOR_TEST_PATTERN_MODE_PN9 -> return "PN9"
            CaptureRequest.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR -> return "SOLID_COLOR"
            else -> return "UNKNOWN" + "-" + sensorAvailableTestPatternModes

        }
    }

    private fun getScalerCroppingType(scalerCroppingType: Int): String {
        when (scalerCroppingType) {
            CaptureRequest.SCALER_CROPPING_TYPE_CENTER_ONLY -> return "CENTER_ONLY"
            CaptureRequest.SCALER_CROPPING_TYPE_FREEFORM -> return "FREEFORM"
            else -> return "UNKNOWN" + "-" + scalerCroppingType

        }
    }

    private fun getRequestAvailableCapabilities(requestAvailableCapabilities: Int): String {
        when (requestAvailableCapabilities) {
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> return "BACKWARD_COMPATIBLE"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> return "BURST_CAPTURE"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> return "CONSTRAINED_HIGH_SPEED_VIDEO"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> return "DEPTH_OUTPUT"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> return "MANUAL_POST_PROCESSING"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> return "LOGICAL_MULTI_CAMERA"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> return "MANUAL_SENSOR"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME -> return "MONOCHROME"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_MOTION_TRACKING -> return "MOTION_TRACKING"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> return "PRIVATE_REPROCESSING"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_RAW -> return "RAW"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> return "READ_SENSOR_SETTINGS"
            CaptureRequest.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> return "YUV_REPROCESSING"
            else -> return "UNKNOWN" + "-" + requestAvailableCapabilities

        }
    }

    private fun getAvailableNoiseReductionModes(availableNoiseReductionModes: Int): String {
        when (availableNoiseReductionModes) {
            CaptureRequest.NOISE_REDUCTION_MODE_FAST -> return "FAST"
            CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY -> return "HIGH_QUALITY"
            CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL -> return "MINIMAL"
            CaptureRequest.NOISE_REDUCTION_MODE_OFF -> return "OFF"
            CaptureRequest.NOISE_REDUCTION_MODE_ZERO_SHUTTER_LAG -> return "ZERO_SHUTTER_LAG"
            else -> return "UNKNOWN" + "-" + availableNoiseReductionModes

        }
    }

    private fun getCameraSensorSyncType(cameraSensorSyncType: Int?): String {
        if (cameraSensorSyncType == null) {
            return "UNKNOWN"
        }
        when (cameraSensorSyncType) {
            CaptureRequest.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE_APPROXIMATE -> return "APPROXIMATE"
            CaptureRequest.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE_CALIBRATED -> return "CALIBRATED"
            else -> return "UNKNOWN" + "-" + cameraSensorSyncType

        }
    }

    private fun getLensPoseReference(lensPoseReference: Int?): String {
        if (lensPoseReference == null) {
            return "UNKNOWN"
        }
        when (lensPoseReference) {
            CaptureRequest.LENS_POSE_REFERENCE_PRIMARY_CAMERA -> return "PRIMARY_CAMERA"
            CaptureRequest.LENS_POSE_REFERENCE_GYROSCOPE -> return "GYROSCOPE"
            else -> return "UNKNOWN" + "-" + lensPoseReference

        }
    }

    private fun getFocusDistanceCalibration(focusDistanceCalibration: Int?): String {
        if (focusDistanceCalibration == null) {
            return "UNKNOWN"
        }
        when (focusDistanceCalibration) {
            CaptureRequest.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_APPROXIMATE -> return "APPROXIMATE"
            CaptureRequest.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_CALIBRATED -> return "CALIBRATED"
            CaptureRequest.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_UNCALIBRATED -> return "UNCALIBRATED"
            else -> return "UNKNOWN" + "-" + focusDistanceCalibration

        }
    }

    private fun getAvailableOpticalStabilization(jsonArrayAvailableOpticalStabilization: Int): String {
        when (jsonArrayAvailableOpticalStabilization) {
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF -> return "OFF"
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON -> return "ON"
            else -> return "UNKNOWN" + "-" + jsonArrayAvailableOpticalStabilization

        }
    }


    private fun getAvailableHotPixelModes(availableHotPixelModes: Int): String {
        when (availableHotPixelModes) {
            CaptureRequest.HOT_PIXEL_MODE_FAST -> return "FAST"
            CaptureRequest.HOT_PIXEL_MODE_HIGH_QUALITY -> return "HIGH_QUALITY"
            CaptureRequest.HOT_PIXEL_MODE_OFF -> return "OFF"
            else -> return "UNKNOWN" + "-" + availableHotPixelModes

        }
    }

    private fun getAvailableEdgeModes(availableEdgeModes: Int): String {
        when (availableEdgeModes) {
            CaptureRequest.EDGE_MODE_FAST -> return "FAST"
            CaptureRequest.EDGE_MODE_HIGH_QUALITY -> return "HIGH_QUALITY"
            CaptureRequest.EDGE_MODE_OFF -> return "OFF"
            CaptureRequest.EDGE_MODE_ZERO_SHUTTER_LAG -> return "ZERO_SHUTTER_LAG"
            else -> return "UNKNOWN" + "-" + availableEdgeModes

        }
    }


    private fun getCorrectionAvailableModes(correctionAvailableModes: Int): String {
        when (correctionAvailableModes) {
            CaptureRequest.DISTORTION_CORRECTION_MODE_FAST -> return "FAST"
            CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY -> return "HIGH_QUALITY"
            CaptureRequest.DISTORTION_CORRECTION_MODE_OFF -> return "OFF"
            else -> return "UNKNOWN" + "-" + correctionAvailableModes

        }
    }

    private fun getAwbAvailableModes(awbAvailableModes: Int): String {
        when (awbAvailableModes) {
            CaptureRequest.CONTROL_AWB_MODE_AUTO -> return "AUTO"
            CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> return "CLOUDY_DAYLIGHT"
            CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT -> return "DAYLIGHT"
            CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT -> return "FLUORESCENT"
            CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT -> return "INCANDESCENT"
            CaptureRequest.CONTROL_AWB_MODE_OFF -> return "OFF"
            CaptureRequest.CONTROL_AWB_MODE_SHADE -> return "SHADE"
            CaptureRequest.CONTROL_AWB_MODE_TWILIGHT -> return "CONTROL_AWB_MODE_TWILIGHT"
            CaptureRequest.CONTROL_AWB_MODE_WARM_FLUORESCENT -> return "WARM_FLUORESCENT"
            else -> return "UNKNOWN" + "-" + awbAvailableModes

        }
    }


    private fun getVideoStabilizationModes(videoStabilizationModes: Int): String {
        when (videoStabilizationModes) {
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF -> return "OFF"
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON -> return "ON"
            else -> return "UNKNOWN" + "-" + videoStabilizationModes

        }
    }

    private fun getAvailableSceneModes(availableSceneModes: Int): String {
        when (availableSceneModes) {
            CaptureRequest.CONTROL_SCENE_MODE_ACTION -> return "ACTION"
            CaptureRequest.CONTROL_SCENE_MODE_BARCODE -> return "BARCODE"
            CaptureRequest.CONTROL_SCENE_MODE_BEACH -> return "BEACH"
            CaptureRequest.CONTROL_SCENE_MODE_CANDLELIGHT -> return "CANDLELIGHT"
            CaptureRequest.CONTROL_SCENE_MODE_DISABLED -> return "DISABLED"
            CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY -> return "FACE_PRIORITY"
            CaptureRequest.CONTROL_SCENE_MODE_FIREWORKS -> return "FIREWORKS"
            CaptureRequest.CONTROL_SCENE_MODE_HDR -> return "HDR"
            CaptureRequest.CONTROL_SCENE_MODE_LANDSCAPE -> return "LANDSCAPE"
            CaptureRequest.CONTROL_SCENE_MODE_NIGHT -> return "NIGHT"
            CaptureRequest.CONTROL_SCENE_MODE_NIGHT_PORTRAIT -> return "NIGHT_PORTRAIT"
            CaptureRequest.CONTROL_SCENE_MODE_PARTY -> return "PARTY"
            CaptureRequest.CONTROL_SCENE_MODE_PORTRAIT -> return "PORTRAIT"
            CaptureRequest.CONTROL_SCENE_MODE_SNOW -> return "SNOW"
            CaptureRequest.CONTROL_SCENE_MODE_SPORTS -> return "SPORTS"
            CaptureRequest.CONTROL_SCENE_MODE_STEADYPHOTO -> return "STEADYPHOTO"
            CaptureRequest.CONTROL_SCENE_MODE_SUNSET -> return "SUNSET"
            CaptureRequest.CONTROL_SCENE_MODE_THEATRE -> return "THEATRE"
            CaptureRequest.CONTROL_SCENE_MODE_HIGH_SPEED_VIDEO -> return "HIGH_SPEED_VIDEO"
            else -> return "UNKNOWN" + "-" + availableSceneModes

        }
    }

    private fun getAvailableModes(availableModes: Int): String {
        return when (availableModes) {
            CaptureRequest.CONTROL_MODE_AUTO -> "AUTO"
            CaptureRequest.CONTROL_MODE_OFF -> "OFF"
            CaptureRequest.CONTROL_MODE_OFF_KEEP_STATE -> "OFF_KEEP_STATE"
            CaptureRequest.CONTROL_MODE_USE_SCENE_MODE -> "MODE_USE_SCENE_MODE"
            else -> "UNKNOWN" + "-" + availableModes
        }
    }

    private fun getAvailableEffects(availableEffects: Int): String {
        when (availableEffects) {
            CaptureRequest.CONTROL_EFFECT_MODE_OFF -> return "OFF"
            CaptureRequest.CONTROL_EFFECT_MODE_AQUA -> return "AQUA"
            CaptureRequest.CONTROL_EFFECT_MODE_BLACKBOARD -> return "BLACKBOARD"
            CaptureRequest.CONTROL_EFFECT_MODE_MONO -> return "MONO"
            CaptureRequest.CONTROL_EFFECT_MODE_NEGATIVE -> return "NEGATIVE"
            CaptureRequest.CONTROL_EFFECT_MODE_POSTERIZE -> return "POSTERIZE"
            CaptureRequest.CONTROL_EFFECT_MODE_SEPIA -> return "SEPIA"
            CaptureRequest.CONTROL_EFFECT_MODE_SOLARIZE -> return "SOLARIZE"
            CaptureRequest.CONTROL_EFFECT_MODE_WHITEBOARD -> return "WHITEBOARD"
            else -> return "UNKNOWN" + "-" + availableEffects

        }
    }

    private fun getAfAvailableModes(afAvailableModes: Int): String {
        when (afAvailableModes) {
            CaptureRequest.CONTROL_AF_MODE_OFF -> return "OFF"
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> return "CONTINUOUS_PICTURE"
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> return "CONTINUOUS_VIDEO"
            CaptureRequest.CONTROL_AF_MODE_EDOF -> return "EDOF"
            CaptureRequest.CONTROL_AF_MODE_MACRO -> return "MACRO"
            CaptureRequest.CONTROL_AF_MODE_AUTO -> return "AUTO"

            else -> return "UNKNOWN" + "-" + afAvailableModes

        }
    }

    private fun getAeAvailableModes(aeAvailableModes: Int): String {
        when (aeAvailableModes) {
            CaptureRequest.CONTROL_AE_MODE_OFF -> return "OFF"
            CaptureRequest.CONTROL_AE_MODE_ON -> return "ON"
            CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH -> return "ON_ALWAYS_FLASH"
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH -> return "ON_AUTO_FLASH"
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE -> return "ON_AUTO_FLASH_REDEYE"
            CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH -> return "ON_EXTERNAL_FLASH"

            else -> return "UNKNOWN" + "-" + aeAvailableModes

        }
    }

    private fun getAntiBandingModes(antiBandingModes: Int): String {
        when (antiBandingModes) {
            CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_50HZ -> return "50HZ"
            CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_60HZ -> return "60HZ"
            CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO -> return "AUTO"
            CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_OFF -> return "OFF"
            else -> return "UNKNOWN" + "-" + antiBandingModes

        }
    }

    private fun getAberrationModes(aberrationModes: Int): String {
        when (aberrationModes) {
            CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_FAST -> return "FAST"
            CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY -> return "HIGH_QUALITY"
            CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF -> return "OFF"
            else -> return "UNKNOWN" + "-" + aberrationModes

        }
    }

    private fun getFacing(facing: Int?): String {
        if (facing == null) {
            return "UNKNOWN"
        }
        when (facing) {
            CameraCharacteristics.LENS_FACING_FRONT -> return "FRONT"
            CameraCharacteristics.LENS_FACING_BACK -> return "BACK"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> return "EXTERNAL"
            else -> return "UNKNOWN" + "-" + facing
        }
    }

    private fun getLevel(level: Int?): String {
        if (level == null) {
            return "UNKNOWN"
        }
        when (level) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> return "LEGACY"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> return "LEVEL_3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> return "EXTERNAL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> return "FULL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> return "LIMITED"
            else -> return "UNKNOWN" + "-" + level
        }
    }

    private fun getFormat(format: Int): String {
        when (format) {
            ImageFormat.DEPTH16 -> return "DEPTH16"
            ImageFormat.DEPTH_POINT_CLOUD -> return "DEPTH_POINT_CLOUD"
            ImageFormat.FLEX_RGBA_8888 -> return "FLEX_RGBA_8888"
            ImageFormat.FLEX_RGB_888 -> return "FLEX_RGB_888"
            ImageFormat.JPEG -> return "JPEG"
            ImageFormat.NV16 -> return "NV16"
            ImageFormat.NV21 -> return "NV21"
            ImageFormat.PRIVATE -> return "PRIVATE"
            ImageFormat.RAW10 -> return "RAW10"
            ImageFormat.RAW12 -> return "RAW12"
            ImageFormat.RAW_PRIVATE -> return "RAW_PRIVATE"
            ImageFormat.RAW_SENSOR -> return "RAW_SENSOR"
            ImageFormat.RGB_565 -> return "RGB_565"
            ImageFormat.YUV_420_888 -> return "YUV_420_888"
            ImageFormat.YUV_422_888 -> return "YUV_422_888"
            ImageFormat.YUV_444_888 -> return "YUV_444_888"
            ImageFormat.YUY2 -> return "YUY2"
            ImageFormat.YV12 -> return "YV12"
            else -> return "UNKNOWN" + "-" + format
        }
    }
}
