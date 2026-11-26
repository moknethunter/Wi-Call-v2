package com.mokasas.wikall

internal const val OVERLAY_NETWORK = "10.126.124.0"
internal const val OVERLAY_PREFIX = 22
internal const val OVERLAY_HOST = "10.126.124.1"
internal const val OVERLAY_INSTANCE = "wikall"

internal data class OverlayRequest(val pin: String, val secret: String, val number: String, val peer: String?, val extraPeers: List<String> = emptyList()) {
    init {
        require(pin.length == 64 && pin.all { it in '0'..'9' || it in 'A'..'F' })
        require(validNonce(secret))
        require(validNumber(number))
        require((listOfNotNull(peer) + extraPeers).all { privateIpv4(it) && !overlayAddress(it) })
    }

    val address: String
        get() {
            val offset = number.toInt() - HOST_NUMBER.toInt() + 1
            return "10.126.${124 + offset / 256}.${offset % 256}"
        }

    fun configuration() = buildString {
        appendLine("inst_name = \"$OVERLAY_INSTANCE\"")
        appendLine("ipv4 = \"$address/$OVERLAY_PREFIX\"")
        appendLine("listeners = [\"udp://0.0.0.0:$EASYTIER_RELAY_PORT\"]")
        appendLine("stun_servers = []")
        appendLine("stun_servers_v6 = []")
        appendLine("[network_identity]")
        appendLine("network_name = \"WiKall-${pin.take(24)}\"")
        appendLine("network_secret = \"$secret\"")
        appendLine("[flags]")
        appendLine("private_mode = true")
        appendLine("enable_exit_node = false")
        appendLine("enable_ipv6 = false")
        appendLine("accept_dns = false")
        (listOfNotNull(peer) + extraPeers).distinct().forEach { endpoint ->
            appendLine("[[peer]]")
            appendLine("uri = \"udp://$endpoint:$EASYTIER_RELAY_PORT\"")
        }
    }
}

internal fun overlayAddress(address: String): Boolean {
    if (!privateIpv4(address)) return false
    val parts = address.split('.')
    return parts[0] == "10" && parts[1] == "126" && parts[2].toInt() in 124..127
}
