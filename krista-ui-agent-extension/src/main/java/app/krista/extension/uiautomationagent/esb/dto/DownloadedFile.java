/*
 * Krista Ui Agent Extension Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extension.uiautomationagent.esb.dto;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * Downloaded file information with HMAC-signed URLs.
 * Matches DownloadedFile from ui_agent_messages.proto
 */
public class DownloadedFile {

    private String filename;
    private String path;
    private long sizeBytes;
    private String mimeType;
    private long downloadedAt;
    private String downloadUrl;
    private long expiresAt;
    private String signature;
    private String sha256Checksum;

    public DownloadedFile() {
    }

    @ProtoField(number = 1)
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @ProtoField(number = 2)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @ProtoField(number = 3)
    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    @ProtoField(number = 4)
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @ProtoField(number = 5)
    public long getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(long downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    @ProtoField(number = 6)
    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @ProtoField(number = 7)
    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    @ProtoField(number = 8)
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @ProtoField(number = 9)
    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public void setSha256Checksum(String sha256Checksum) {
        this.sha256Checksum = sha256Checksum;
    }

    @Override
    public String toString() {
        return "DownloadedFile{" +
                "filename='" + filename + '\'' +
                ", path='" + path + '\'' +
                ", sizeBytes=" + sizeBytes +
                ", mimeType='" + mimeType + '\'' +
                ", downloadedAt=" + downloadedAt +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", expiresAt=" + expiresAt +
                ", signature='" + signature + '\'' +
                ", sha256Checksum='" + sha256Checksum + '\'' +
                '}';
    }
}

