package se.fredrikwiman.s3projekt;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@SpringBootApplication
public class S3ProjektApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(S3ProjektApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        s3Service s3Service = new s3Service();

        s3Service.chooseBucket();


        // GÖR OM TILL EN FUNKTION ISTÄLLET FÖR CLEAN CODE??????+
        while (true) {
            System.out.println("1. Lista alla filer");
            System.out.println("2. Ladda upp filer");
            System.out.println("3. Ladda ner filer");
            System.out.println("4. Ta bort fil");
            System.out.println("5. Sök efter fil");
            System.out.println("6. Zip:a och ladda upp fil");
            System.out.println("7. Avsluta");
            System.out.println("Choose an option: ");

            String input = scanner.nextLine();
            int choice = Integer.parseInt(input);
            switch (choice) {
                case 1:
                    s3Service.ListAll();
                    break;
                case 2:
                    System.out.println("Ange filens fulla sökväg: ");
                    String filePath = scanner.nextLine();

                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        String key = file.getName();
                        s3Service.uploadFile(key, file.getAbsolutePath());
                    } else {
                        System.out.println("Ingen fil hittades: " + filePath);
                    }
                    break;
                case 3:
                    System.out.println("Vilken fil vill du ladda ner? (Ange filnamn från s3 bucket)");
                    s3Service.ListAll();
                    String key = scanner.nextLine();

                    System.out.println("Ange var du vill spara filen: ");
                    String destination = scanner.nextLine();

                    s3Service.downloadFile(key, destination);
                    break;
                case 4:
                    System.out.println("Vilken fil vill du ta bort?(Ange filnamn från s3 bucket)");
                    s3Service.ListAll();
                    String deleteInput = scanner.nextLine();

                    s3Service.deleteFile(deleteInput);
                    break;
                case 5:
                    System.out.println("Ange vad du söker efter: ");
                    String searchInput = scanner.nextLine();

                    s3Service.findFile(searchInput);
                    break;
                case 6:
                    System.out.println("Ange filens fulla sökväg: ");
                    String folderPath = scanner.nextLine();

                    File folder = new File(folderPath);
                    if (folder.exists() && folder.isDirectory()) {
                        String zipKey = folder.getName() + ".zip";
                        s3Service.zipAndUploadFolder(folder.getAbsolutePath());
                    } else {
                        System.out.println("Ingen fil hittades: " + folderPath);
                    }
                    break;
                case 7:
                    return;
            }
        }
    }
}
