package dev.fishies.coho

import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

/**
 * @suppress
 */
class DownloadElement(val url: URL, val destination: String? = null, val useCache: Boolean) : Element(url.toString()) {
    override fun _generate(location: Path): List<Path> {
        val dest = location.resolve(destination ?: url.file.split("/").last())
        val data: ByteArray = if (useCache) {
            val cacheFile =
                Paths.get(System.getProperty("java.io.tmpdir")).resolve(url.toString().hashCode().toHexString())
            if (cacheFile.exists()) {
                cacheFile.readBytes()
            } else {
                url.openStream().readBytes().apply(cacheFile::writeBytes)
            }
        } else {
            url.openStream().readBytes()
        }
        dest.writeBytes(data)
        return listOf(dest)
    }
}

/**
 * Download a file from the given [url] to [destination].
 * If [destination] is `null`, the last section of the URL will be used.
 * By default, files are cached to the temporary directory so later runs won't download anything, but it can be disabled
 * with [useCache].
 */
fun OutputPath.dl(url: URL, destination: String? = null, useCache: Boolean = true) =
    children.add(DownloadElement(url, destination, useCache))

/**
 * Download a file from the given [url] to [destination].
 * If [destination] is `null`, the last section of the URL will be used.
 * By default, files are cached to the temporary directory so later runs won't download anything, but it can be disabled
 * with [useCache].
 */
fun OutputPath.dl(url: String, destination: String? = null, useCache: Boolean = true) =
    dl(URI(url).toURL(), destination, useCache)