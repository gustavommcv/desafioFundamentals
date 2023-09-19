package com.gustavo;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gustavo.models.User;

public class SimpleHttpClient {
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {

        // Abrindo um novo client para fazer as requisicoes
        HttpClient httpClient = HttpClient.newHttpClient();

        Scanner scan = new Scanner(System.in);
        String op;

        var on = true;

        while(on) {
            System.out.println("1: CADASTRAR USUARIO / 2: BUSCAR USUARIO / -1: SAIR");

            op = scan.next();

            scan.nextLine();

            switch (op) {
                case "1":
                    System.out.println("Digite o login:");
                    var login = scan.nextLine();

                    System.out.println("Digite o email:");
                    var email = scan.nextLine();

                    System.out.println("Digite o login:");
                    var password = scan.nextLine();

                    // Criando uma request tipo post
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URI("https://postman-echo.com/post"))
                            .POST(HttpRequest.BodyPublishers.ofString(String.format("{\"login\": \"%s\",\"email\": \"%s\",\"password\": \"%s\"}", login, email, password)))
                            .build();

                    // Armazenando a resposta
                    HttpResponse<String> response =
                            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    int statusCode = response.statusCode();
                    String responseBody = response.body();
                    System.out.println("Status Code: " + statusCode);
                    System.out.println("Response Body: " + responseBody);

                    // Analisa o JSON do corpo de resposta
                    ObjectMapper objectMapper = new ObjectMapper(); // ObjectMapper é uma classe da biblioteca Jackson que pode transformar classes POJO (Plain Old Java Objects) em JSON (JavaScript Object Notation) e vice-versa.
                    JsonNode jsonNode = objectMapper.readTree(responseBody); // JsonNode é parte de uma árvore JSON, onde cada nó representa um valor JSON

                    // Armazenar os campos JSON específicos
                    String loginJson = jsonNode.at("/data/login").asText();
                    String emailJson = jsonNode.at("/data/email").asText();
                    String passwordJson = jsonNode.at("/data/password").asText();

                    var id = generateId(); // Gerando um id auto-incrementavel

                    if (id != -1)
                        serialize(new User(id, loginJson, passwordJson, emailJson)); // Gera um arquivo serializado com o usuario passado
                    else
                        System.out.println("Nao foi possivel criar o usuario");

                    break;

                case "2":
                    System.out.println("Digite o id do usuario que deseja buscar:");
                    var idInput = scan.nextInt();
                    scan.nextLine();

                    searchUserById(idInput); // Busca em um arquivo serializado o usuario

                    break;

                case "-1":
                    on = false;
                    System.out.println("Saindo . . .");
                    break;
                default:
                    System.out.println("Opcao invalida!");
            }
        }

        scan.close();
    }

    static void serialize(User user) throws IOException {
        String folderName = "data";
        String fileName = user.getId() + ".s"; // ID como parte do nome do arquivo

        Path currentPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources");
        Path folderPath = currentPath.resolve(folderName);
        Path filePath = folderPath.resolve(fileName);

        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(filePath))) {
            oos.writeObject(user);

            System.out.println("Objeto serializado com sucesso para o arquivo: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void searchUserById(int userId) {
        String folderName = "data";
        String fileName = userId + ".s";

        Path currentPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources");
        Path folderPath = currentPath.resolve(folderName);
        Path filePath = folderPath.resolve(fileName);

        if (!Files.exists(filePath)) {
            System.out.println("Arquivo não encontrado para o usuário com ID: " + userId);
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(filePath))) {
            User user = (User) ois.readObject();
            System.out.println("Usuário recuperado do arquivo: " + filePath);
            System.out.println(user);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    static int generateId() throws IOException {
        String folderName = "auto-increment";
        String fileName = "current-id";

        Path currentPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources");
        Path folderPath = currentPath.resolve(folderName);
        Path filePath = folderPath.resolve(fileName);

        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
        }

        try {
            // Lê o valor atual do arquivo "id"
            int valorAtual = lerValorAtual(filePath);

            // Incrementa o valor
            int novoValor = valorAtual + 1;

            // Escreve o novo valor de volta no arquivo
            escreverNovoValor(filePath, novoValor);

            return novoValor;
        } catch (IOException e) {
            e.printStackTrace();
            return -1; // Retorna -1 em caso de erro
        }
    }

    static int lerValorAtual(Path filePath) throws IOException {
        if (Files.exists(filePath)) {
            try (BufferedReader br = Files.newBufferedReader(filePath)) {
                String linha = br.readLine();
                if (linha != null) {
                    return Integer.parseInt(linha);
                }
            }
        }
        return 0; // Valor padrão se o arquivo não existir
    }

    static void escreverNovoValor(Path filePath, int novoValor) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write(Integer.toString(novoValor));
        }
    }
}
