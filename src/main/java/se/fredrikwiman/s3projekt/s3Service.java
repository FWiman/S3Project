package se.fredrikwiman.s3projekt;

import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class s3Service {
    // LÄGG ALLA METODER HÄR ISTÄLLET FÖR MAIN

    Dotenv dotenv = Dotenv.load();
    String bucketName = dotenv.get("BUCKET_NAME");
    String accessKey = dotenv.get("ACCESS_KEY");
    String secretKey = dotenv.get("SECRET_KEY");


    S3Client s3Client = S3Client.builder()
            .region(Region.EU_NORTH_1)
            .credentialsProvider(new AwsCredentialsProvider() {
                @Override
                public AwsCredentials resolveCredentials() {
                    return AwsBasicCredentials.builder()
                            .accessKeyId(accessKey)
                            .secretAccessKey(secretKey).build();
                }
            }).build();


    public String chooseBucket() {
        System.out.println("---------------------------------");
        System.out.println("Vilken bucket vill du jobba emot?");
        System.out.println("---------------------------------");


        List<Bucket> buckets = s3Client.listBuckets().buckets();

        if (buckets.isEmpty()) {
            System.out.println("Inga buckets hittades");
            return null;
        } else {
            System.out.println("Hittade " + buckets.size() + " buckets");
            System.out.println("-------------------------------");
            for (Bucket bucket : buckets) {
                System.out.println(bucket.name());
            }
        }
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Välj en bucket att jobba emot: (1-" + buckets.size() + ")");
            String input = scanner.nextLine();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= buckets.size()) {
                    String selectedBucket = buckets.get(choice - 1).name();
                    System.out.println("Du valde bucket: " + selectedBucket);
                    bucketName = selectedBucket;
                    return bucketName;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ogiltigt val. försök igen!");
            }
        }
    }


    // LISTAR ALLA OBJEKT I S3 BUCKET
    public List<String> ListAll() {
        System.out.println("--------------------");
        System.out.println("Nu listas alla filer");
        System.out.println("--------------------");

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);
        List<String> fileNames = listRes.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());

        fileNames.forEach(System.out::println);
        System.out.println("-----------------");
        return List.of();
    }

    // LADDAR UPP FIL TILL S3BUCKET
    public void uploadFile(String key, String filePath) {

        System.out.println("------------");
        System.out.println("Nu laddas filen upp...");
        System.out.println("------------");

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.putObject(putObjectRequest, Paths.get(filePath));

        } catch (Exception e) {
            System.err.println("Fel vid uppladdning till s3: " + e.getMessage());
            System.exit(1);
        }
    }

    // LADDAR NER FIL FRÅN S3BUCKET
    public void downloadFile(String key, String destinationPath) {
        System.out.println("------------");
        System.out.println("Nu laddas filen ner...");
        System.out.println("------------");

        Path destination = Paths.get(destinationPath);
        if (Files.isDirectory(destination)) {
            destination = destination.resolve(key);
        }
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(destination));

            System.out.println("Filen laddades ner till: " + destination);

        } catch (Exception e) {
            System.err.println("Fel vid nedladdning: " + e.getMessage());
            System.exit(1);
        }
    }

    // TAR BORT FIL FRÅN S3BUCKET
    public void deleteFile(String key) {
        System.out.println("------------");
        System.out.println("filen tas bort...");
        System.out.println("------------");

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            System.err.println("Fel borttagning av fil: " + e.getMessage());
            System.exit(1);
        }
    }

    // SÖKFUNKTION SOM MATCHAR EN DEL AV KEY:N OMGJORD TILL LOWERCASE FÖR ATT VARA CASE-INSENSITIVE MOT OBJEKT I S3BUCKET OCH LISTAR ALLA RESULTAT
    public void findFile(String partOfKey) {
        System.out.println("------------");
        System.out.println("Söker efter filen...");
        System.out.println("------------");

        String lowerCaseKey = partOfKey.toLowerCase();

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            List<S3Object> matchingFiles = listResponse.contents()
                    .stream()
                    .filter(obj -> obj.key().toLowerCase().contains(lowerCaseKey))
                    .collect(Collectors.toList());

            if (matchingFiles.isEmpty()) {
                System.out.println("Inga matchningar hitades: " + lowerCaseKey);
            } else {
                System.out.println("Följande filer matchade: ");
                matchingFiles.forEach(obj ->
                        System.out.println(obj.key() + " (" + obj.size() + " bytes)")
                );
            }

        } catch (Exception e) {
            System.err.println("Fel vid sökning: " + e.getMessage());
        }

    }

    // ZIPPAR EN HEL MAPP TILL EN ZIP-FIL
    public Path zipFolder(Path sourceDirPath) throws IOException {
        String zipFilePath = sourceDirPath.toString() + ".zip";
        Path zipPath = Paths.get(zipFilePath);

        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Files.walk(sourceDirPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println("Error during zipping: " + e.getMessage());
                        }
                    });
        }
        System.out.println("Folder zipped to: " + zipPath.toAbsolutePath());
        return zipPath;
    }

    // ZIPPAR OCH LADDAR UPP EN HEL MAPP TILL S3BUCKET
    public void zipAndUploadFolder(String folderPath) throws IOException {
        try {
            Path sourceDirPath = Paths.get(folderPath);
            if (!Files.isDirectory(sourceDirPath)) {
                System.out.println("Please provide a valid folder path");
                return;
            }

            Path zipPath = zipFolder(sourceDirPath);
            String key = zipPath.getFileName().toString();
            uploadFile(key, zipPath.toAbsolutePath().toString());

            Files.delete(zipPath);
            System.out.println("Temporary zip deleted.");
        } catch (IOException e) {
            System.err.println("Error during zipping and uploading: " + e.getMessage());
        }
    }

}
