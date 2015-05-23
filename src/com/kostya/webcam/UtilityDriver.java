package com.kostya.webcam;

import android.content.Context;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentList;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Kostya on 20.12.2014.
 */
public class UtilityDriver {

    static final String folderMIME = "application/vnd.google-apps.folder";
    public static final int REQUEST_AUTHORIZATION = 1990;

    private GoogleAccountCredential credential;
    private Drive driveService;

    public UtilityDriver(Context context, String accountName) {

        credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE));
        credential.setSelectedAccountName(accountName);
        driveService = getDriveService(credential);

    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new JacksonFactory(), credential).build();
    }

    public File getFile(String id) throws IOException {
        return driveService.files().get(id).execute();
    }

    public ParentList getParents(String id) throws IOException {
        return driveService.parents().list(id).execute();
    }

    public List<File> getFileList(String q) throws IOException {

        List<File> result = new ArrayList<File>();
        com.google.api.services.drive.Drive.Files.List listRequest = driveService.files().list();
        if (q != null && q.length() > 0)
            listRequest.setQ(q);

        do {

            FileList fList = listRequest.execute();
            result.addAll(fList.getItems());
            listRequest.setPageToken(fList.getNextPageToken());

        } while (listRequest.getPageToken() != null && listRequest.getPageToken().length() > 0);

        return result;

    }

    public void deleteFile(String fileId) throws IOException {

        driveService.files().delete(fileId).execute();

    }

    public InputStream downloadFile(File file) throws IOException {
        HttpResponse resp = driveService.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
        return resp.getContent();
    }

    public File uploadFile(String title, String desc, String parentId, String mime, String fName) throws IOException {

        File body = new File();
        body.setTitle(title);
        body.setDescription(desc);
        body.setMimeType(mime);

        if (parentId != null && parentId.length() > 0) {

            ParentReference parent = new ParentReference();
            parent.setId(parentId);
            body.setParents(Arrays.asList(parent));

        }

        java.io.File content = new java.io.File(fName);
        FileContent fContent = new FileContent(mime, content);

        return driveService.files().insert(body, fContent).execute();
    }

    public File uploadFile(String title, /*String desc,*/ String parentId, String mime, java.io.File fName) throws IOException {

        com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
        body.setTitle(title);
        //body.setDescription(desc);
        body.setMimeType(mime);

        if (parentId != null && parentId.length() > 0) {

            ParentReference parent = new ParentReference();
            parent.setId(parentId);
            body.setParents(Arrays.asList(parent));

        }

        FileContent fContent = new FileContent(mime, fName);

        return driveService.files().insert(body, fContent).execute();
    }

    public File createFolder(String parentId, String folderName) throws IOException {
        File body = new File();
        body.setTitle(folderName);
        body.setMimeType(folderMIME);

        if (parentId != null && parentId.length() > 0) {

            ParentReference parent = new ParentReference();
            parent.setId(parentId);
            body.setParents(Arrays.asList(parent));
        }

        return driveService.files().insert(body).execute();
    }

    public File updateFile(String fileId, String newTitle, String newDesc, String newMime, String newFileName) throws IOException {

        File oldFile = driveService.files().get(fileId).execute();
        oldFile.setTitle(newTitle);
        oldFile.setDescription(newDesc);
        oldFile.setMimeType(newMime);

        java.io.File newFile = new java.io.File(newFileName);
        FileContent newContent = new FileContent(newMime, newFile);

        return driveService.files().update(fileId, oldFile, newContent).execute();

    }

    public File getFolder(String folder, String parentId) throws IOException {
        List<com.google.api.services.drive.model.File> result = new ArrayList<com.google.api.services.drive.model.File>();
        com.google.api.services.drive.Drive.Files.List listRequest = driveService.files().list();
        //listRequest.setMaxResults(10);
        StringBuilder stringBuilder = new StringBuilder("mimeType = 'application/vnd.google-apps.folder'");
        stringBuilder.append(" and title = " + "\'" + folder + "\'");
        stringBuilder.append(" and trashed = false");
        if (parentId != null)
            stringBuilder.append(" and " + "\'" + parentId + "\'" + " in parents");
        //listRequest.setQ("mimeType = 'application/vnd.google-apps.folder' and title = " + "\'" + folder + "\'" + " and trashed = false");
        listRequest.setQ(stringBuilder.toString());
        do {
            FileList fList = listRequest.execute();
            result.addAll(fList.getItems());
            listRequest.setPageToken(fList.getNextPageToken());

        } while (listRequest.getPageToken() != null && listRequest.getPageToken().length() > 0);

        if (result.isEmpty()) {
            return createFolder(parentId, folder);
        }

        return result.get(0);
    }
}
