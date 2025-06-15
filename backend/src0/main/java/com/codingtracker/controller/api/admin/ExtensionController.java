package com.codingtracker.controller.api.admin;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/admin/extension")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class ExtensionController {

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadExtension() {
        try {
            // 创建临时zip文件
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // 扩展文件的基础路径
            String extensionPath = "src/main/resources/static/browser-extension/";

            // 要包含的文件列表
            String[] files = {
                    "manifest.json",
                    "popup.html",
                    "popup.js",
                    "background.js",
                    "content.js",
                    "utils.js",
                    "README.md",
                    "INSTALL.md",
                    "generate-icons.html",
                    "create-temp-icons.html"
            };

            // 添加文件到zip
            for (String fileName : files) {
                addFileToZip(zos, extensionPath + fileName, fileName);
            }

            // 添加manifest内容
            addManifestToZip(zos);

            zos.close();

            ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=coding-tracker-token-helper.zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void addFileToZip(ZipOutputStream zos, String filePath, String entryName) throws IOException {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                Files.copy(path, zos);
                zos.closeEntry();
            }
        } catch (Exception e) {
            // 如果文件不存在，跳过
            System.out.println("文件不存在，跳过: " + filePath);
        }
    }

    private void addManifestToZip(ZipOutputStream zos) throws IOException {
        String manifestContent = """
                {
                  "manifest_version": 3,
                  "name": "Coding Tracker Token Helper",
                  "version": "1.0",
                  "description": "自动获取OJ平台的Token用于Coding Tracker",
                  "permissions": [
                    "activeTab",
                    "cookies",
                    "storage"
                  ],
                  "host_permissions": [
                    "https://www.luogu.com.cn/*",
                    "https://leetcode.com/*",
                    "https://leetcode.cn/*",
                    "https://codeforces.com/*",
                    "https://atcoder.jp/*",
                    "https://www.topcoder.com/*",
                    "https://vjudge.net/*",
                    "https://www.spoj.com/*",
                    "https://www.codechef.com/*"
                  ],
                  "action": {
                    "default_popup": "popup.html",
                    "default_title": "获取Token"
                  },
                  "content_scripts": [
                    {
                      "matches": [
                        "https://www.luogu.com.cn/*",
                        "https://leetcode.com/*",
                        "https://leetcode.cn/*",
                        "https://codeforces.com/*",
                        "https://atcoder.jp/*",
                        "https://www.topcoder.com/*",
                        "https://vjudge.net/*",
                        "https://www.spoj.com/*",
                        "https://www.codechef.com/*"
                      ],
                      "js": ["content.js"]
                    }
                  ],
                  "background": {
                    "service_worker": "background.js"
                  }
                }
                """;

        ZipEntry entry = new ZipEntry("manifest.json");
        zos.putNextEntry(entry);
        zos.write(manifestContent.getBytes());
        zos.closeEntry();
    }
}