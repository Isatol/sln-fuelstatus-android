package mx.fuelstatus.backup

import kotlinx.coroutines.runBlocking
import mx.fuelstatus.Preferences
import mx.fuelstatus.data.Vehicle
import org.junit.Assert.*
import org.junit.Test

class DriveApiTest {
    @Test fun accountLookupAndFolderCreationRequestOnlyNeededFields() = runBlocking {
        val requests = mutableListOf<DriveRequest>()
        val api = DriveApi(DriveTransport { request ->
            requests += request
            val json = when {
                request.path == "about" -> """{"user":{"emailAddress":"person@example.com"}}"""
                request.method == "GET" -> """{"files":[]}"""
                else -> """{"id":"folder-id"}"""
            }
            DriveResponse(200, json.toByteArray())
        })
        assertEquals("person@example.com", api.email())
        assertEquals("folder-id", api.folder())
        assertEquals("user(emailAddress)", requests[0].query["fields"])
        assertTrue(requests[1].query.getValue("q").contains("fuelstatusFolder"))
        val metadata = requests[2].body!!.toString(Charsets.UTF_8)
        assertTrue(metadata.contains("application/vnd.google-apps.folder"))
        assertTrue(metadata.contains("FuelStatus"))
    }
    @Test fun multipartUploadIncludesFullCopyAndContentIdentity() = runBlocking {
        val doc = BackupDocument(1_770_000_000_000, "phone", 7, listOf(Vehicle("car")), emptyList(), emptyList(), Preferences())
        val backup = LocalBackup(doc, BackupCodec.encode(doc))
        var sent: DriveRequest? = null
        val api = DriveApi(DriveTransport { request ->
            sent = request
            DriveResponse(200, """{"id":"copy-id","createdTime":"2026-02-02T00:00:00.000Z","md5Checksum":"${backup.md5}","appProperties":{"clientBackupId":"${backup.id}","revision":"7","deviceId":"phone"}}""".toByteArray())
        })
        val result = api.upload("folder-id", backup)
        val request = sent!!
        assertEquals("POST", request.method); assertTrue(request.upload)
        assertEquals("multipart", request.query["uploadType"])
        val body = request.body!!.toString(Charsets.UTF_8)
        assertTrue(body.contains("\"parents\":[\"folder-id\"]"))
        assertTrue(body.contains(backup.id)); assertTrue(body.contains(backup.bytes.toString(Charsets.UTF_8)))
        assertEquals(backup.id, result.clientId); assertEquals(backup.md5, result.md5); assertEquals(7L, result.revision)
    }
    @Test fun backupListingPaginatesAndSortsByServerDate() = runBlocking {
        val requests = mutableListOf<DriveRequest>()
        val api = DriveApi(DriveTransport { request ->
            requests += request
            val body = if (request.query["pageToken"] == null)
                """{"nextPageToken":"page2","files":[{"id":"old","createdTime":"2026-01-01T00:00:00Z","md5Checksum":"old-md5","appProperties":{}}]}"""
            else """{"files":[{"id":"new","createdTime":"2026-02-01T00:00:00Z","md5Checksum":"new-md5","appProperties":{}}]}"""
            DriveResponse(200, body.toByteArray())
        })
        assertEquals(listOf("new", "old"), api.backups("folder-id").map { it.id })
        assertEquals("page2", requests[1].query["pageToken"])
        assertTrue(requests[0].query.getValue("q").contains("'folder-id' in parents"))
    }
}
