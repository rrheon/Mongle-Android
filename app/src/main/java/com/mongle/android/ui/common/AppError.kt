package com.mongle.android.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * iOS AppError와 동일한 통합 에러 타입.
 * 네트워크/도메인 레이어에서 발생한 모든 에러를 이 타입으로 변환해 UI에서 일관되게 처리한다.
 */
sealed class AppError {
    data object Offline : AppError()
    data object Timeout : AppError()
    data object Unauthorized : AppError()
    data object NotFound : AppError()
    data class ServerError(val statusCode: Int) : AppError()
    data class Network(val detail: String) : AppError()
    data object Decoding : AppError()
    data class Domain(val message: String) : AppError()
    data class Unknown(val message: String) : AppError()

    /** 토스트에 표시할 짧은 한 줄 메시지 */
    val toastMessage: String
        get() = when (this) {
            is Offline -> "인터넷 연결을 확인해주세요"
            is Timeout -> "서버 응답이 오래 걸리고 있어요"
            is Unauthorized -> "로그인이 필요해요"
            is NotFound -> "요청한 데이터를 찾을 수 없어요"
            is ServerError -> "서버 오류가 발생했어요 ($statusCode)"
            is Network -> "네트워크 오류가 발생했어요"
            is Decoding -> "데이터를 읽는 중 오류가 발생했어요"
            is Domain -> message
            is Unknown -> message.ifEmpty { "알 수 없는 오류가 발생했어요" }
        }

    /** 사용자에게 표시할 상세 메시지 */
    val userMessage: String
        get() = when (this) {
            is Offline -> "인터넷에 연결되어 있지 않아요.\n연결 상태를 확인한 후 다시 시도해 주세요."
            is Timeout -> "서버 응답이 너무 오래 걸리고 있어요.\n잠시 후 다시 시도해 주세요."
            is Unauthorized -> "로그인이 필요해요."
            is NotFound -> "요청한 데이터를 찾을 수 없어요."
            is ServerError -> "서버에 문제가 발생했어요 ($statusCode).\n잠시 후 다시 시도해 주세요."
            is Network -> "네트워크 오류가 발생했어요.\n$detail"
            is Decoding -> "데이터를 읽는 중 오류가 발생했어요."
            is Domain -> message
            is Unknown -> message.ifEmpty { "알 수 없는 오류가 발생했어요." }
        }

    /** 재시도 버튼 표시 여부 */
    val isRetryable: Boolean
        get() = when (this) {
            is Offline, is Timeout, is ServerError, is Network -> true
            else -> false
        }

    /** 로그인 화면 이동 필요 여부 */
    val requiresLogin: Boolean
        get() = this is Unauthorized

    /** 아이콘 */
    val icon: ImageVector
        get() = when (this) {
            is Offline -> Icons.Default.SignalWifiOff
            is Timeout -> Icons.Default.Timer
            is Unauthorized -> Icons.Default.Lock
            is NotFound -> Icons.Default.Search
            is ServerError -> Icons.Default.Warning
            is Network -> Icons.Default.CloudOff
            is Decoding -> Icons.Default.Error
            is Domain, is Unknown -> Icons.Default.Error
        }

    companion object {
        /** 임의의 Exception을 AppError로 변환 */
        fun from(error: Throwable): AppError {
            // HttpException (Retrofit)
            if (error is HttpException) {
                val code = error.code()
                val body = error.response()?.errorBody()?.string()
                return when {
                    code == 401 -> Unauthorized
                    code == 404 -> NotFound
                    code in 400..499 && !body.isNullOrBlank() -> Domain(body)
                    code >= 500 -> ServerError(code)
                    else -> Unknown(body ?: error.message())
                }
            }
            // 네트워크 에러
            if (error is UnknownHostException) return Offline
            if (error is SocketTimeoutException) return Timeout
            if (error is java.net.ConnectException) return Offline

            // 도메인 에러 메시지가 한국어인 경우 그대로 사용
            val msg = error.message ?: ""
            if (msg.any { it in '\uAC00'..'\uD7A3' }) return Domain(msg)

            return Unknown(msg)
        }
    }
}
