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
 * Extension identity information for multi-extension isolation.
 * Matches ExtensionIdentity from ui_agent_messages.proto
 */
public class ExtensionIdentity {

    private String extensionId;
    private String extensionName;
    private String extensionVersion;
    private String userId;
    private String teamId;

    public ExtensionIdentity() {
    }

    @ProtoField(number = 1)
    public String getExtensionId() {
        return extensionId;
    }

    public void setExtensionId(String extensionId) {
        this.extensionId = extensionId;
    }

    @ProtoField(number = 2)
    public String getExtensionName() {
        return extensionName;
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }

    @ProtoField(number = 3)
    public String getExtensionVersion() {
        return extensionVersion;
    }

    public void setExtensionVersion(String extensionVersion) {
        this.extensionVersion = extensionVersion;
    }

    @ProtoField(number = 4)
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @ProtoField(number = 5)
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    @Override
    public String toString() {
        return "ExtensionIdentity{" +
                "extensionId='" + extensionId + '\'' +
                ", extensionName='" + extensionName + '\'' +
                ", extensionVersion='" + extensionVersion + '\'' +
                ", userId='" + userId + '\'' +
                ", teamId='" + teamId + '\'' +
                '}';
    }
}

