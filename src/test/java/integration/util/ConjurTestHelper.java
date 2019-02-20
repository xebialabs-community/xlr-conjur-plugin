/**
 * Copyright 2019 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package integration.util;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.google.common.collect.Maps;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public final class ConjurTestHelper {

    private static final String BASE_URI = "http://localhost:15516/api/v1";
    private static RequestSpecification httpRequest = null;
    private static final String IMPORT_CONFIG = "/config";
    private static final String IMPORT_TEMPLATE = "/templates/import";
    private static final String START_RELEASE_CONJUR = "/templates/Applications/Release1a90039757eb40eca2ea4904e60651a3/start";
    private static final String GET_RELEASE_PREFIX = "/releases/";

    private ConjurTestHelper() {
        /*
         * Private Constructor will prevent the instantiation of this class directly
         */
    }

    static {
        baseURI = BASE_URI;
        // Configure authentication
        httpRequest = given().auth().preemptive().basic("admin", "admin");
    }

    public static Map<String, String> initializeConjur() throws InterruptedException {

        Map<String, String> conjurMap = Maps.newHashMap();

        System.out.println("Sleeping for 15 seconds so Conjur can start.");
        Thread.sleep(15000);

        // create the quick-start acct and get the admin user key
        String result = getCommandResult("docker exec conjur_server bash -c 'conjurctl account create quick-start'");
        String adminKey = result.substring(result.lastIndexOf(":") + 2);
        System.out.println("The Conjur Admin (admin) key = " + adminKey);
        conjurMap.put("adminKey", adminKey);

        // Initial the quick-start acct and log in
        result = getCommandResult("docker exec conjur_client bash -c 'conjur init -u conjur -a quick-start'");
        result = getCommandResult(
                "docker exec conjur_client bash -c 'conjur authn login -u admin -p " + adminKey + "'");

        // Load policy files
        // Load the conjur.yml policy file
        result = getCommandResult("docker exec conjur_client bash -c 'conjur policy load --replace root conjur.yml'");
        // Create host and get host Key
        result = getCommandResult("docker exec conjur_client bash -c 'conjur policy load xld xld.yml'");
        String hostKey = result.substring((result.indexOf("\"api_key\": \"") + 12), result.indexOf("version") - 13);
        System.out.println("The Host (xld/xld-01) key = " + hostKey);
        conjurMap.put("hostKey", hostKey);

        // Load db.yml file
        result = getCommandResult("docker exec conjur_client bash -c 'conjur policy load db db.yml'");

        // Add secrets
        result = getCommandResult("docker exec conjur_client bash -c 'conjur variable values add db/password "
                + "secretPassword123" + "'");
        result = getCommandResult(
                "docker exec conjur_client bash -c 'conjur variable values add db/username " + "theDBUser" + "'");
        result = getCommandResult(
                "docker exec conjur_client bash -c 'conjur variable values add db/tempPath " + "/tmp" + "'");

        return conjurMap;
    }

    public static void initializeXLR() throws InterruptedException{
        System.out.println("Pausing for 30, waiting for XLR to start. ");
        Thread.sleep(30000);
        // Load the conjur server configs 
        // Prepare httpRequest
        JSONObject requestParams = getRequestParamsFromFile(getResourceFilePath("docker/initialize/data/server-configs-Conjur.json"));
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Accept", "application/json");
        httpRequest.body(requestParams.toJSONString());
        
        // Post server config
        Response response = httpRequest.post(IMPORT_CONFIG);
        if (response.getStatusCode() != 200) {
            System.out.println("Status line, import conjur server was " + response.getStatusLine() + "");
        } else {
            //String responseId = response.jsonPath().getString("id");
        }

        // Update the request params and load the moockJira server configs
        requestParams = getRequestParamsFromFile(getResourceFilePath("docker/initialize/data/server-configs-MockJira.json"));
        httpRequest.body(requestParams.toJSONString());

        // Post server config
        response = httpRequest.post(IMPORT_CONFIG);
        if (response.getStatusCode() != 200) {
            System.out.println("Status line, import mockJira was " + response.getStatusLine() + "");
        } else {
            //String responseId = response.jsonPath().getString("id");
        }
        

        try {
            // Load the template
        requestParams = new JSONObject();
        httpRequest.body(requestParams.toJSONString());
        httpRequest.contentType("multipart/form-data");
        httpRequest.multiPart(new File(getResourceFilePath("docker/initialize/data/release-template-Conjur.json")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Load the template
        requestParams = new JSONObject();
        httpRequest.body(requestParams.toJSONString());
        httpRequest.multiPart(new File(getResourceFilePath("docker/initialize/data/release-template-Conjur.json")));

        // Post template
        response = httpRequest.post(IMPORT_TEMPLATE);
        if (response.getStatusCode() != 200) {
            System.out.println("Status line, import template was " + response.getStatusLine() + "");
        } else {
            //String responseId = response.jsonPath().getString("id");
        }
    }

    public static String getConjurReleaseResult() throws InterruptedException{
        String releaseResult = "";
        String responseId = "";
        // Prepare httpRequest, start the release
        JSONObject requestParams = getRequestParams();
        Response response = given().auth().preemptive().basic("admin", "admin")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .body(requestParams.toJSONString())
            .post(START_RELEASE_CONJUR);

        ///////// retrieve the planned release id.
        if (response.getStatusCode() != 200) {
            System.out.println("Status line, Start conjur was " + response.getStatusLine() );
        } else {
            responseId = response.jsonPath().getString("id");
        }

        ///////// Get Archived responses
        // Sleep so XLR can finish processing releases
        System.out.println("Pausing for 15 seconds, waiting for release to complete. If most requests fail with 404, consider sleeping longer.");
        Thread.sleep(15000);
        //////////
        response = given().auth().preemptive().basic("admin", "admin")
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .body(requestParams.toJSONString())
        .get(GET_RELEASE_PREFIX + responseId);

        if (response.getStatusCode() != 200) {
            System.out.println("Status line for get archived conjur was " + response.getStatusLine() + "");
        } else {
            releaseResult = response.jsonPath().get("phases[0].tasks[1].comments[0].text").toString();
        }
        // Return the comments attached to the finished release so we can test for success
        return releaseResult;
    }

    /////////////////// Util methods

    // Runs a command on Unix flavor or Windows
    // Returns the successful string output or will print out error and then create
    // an assert failure
    public static String getCommandResult(String cmd) {
        String returnStr = "";
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        String command = cmd;
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        try {
            Process process = builder.start();
            int exitCode = process.waitFor();
            BufferedReader is = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorIs = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String testLine;

            if (exitCode == 0) {
                while ((testLine = is.readLine()) != null) {
                    returnStr = returnStr + testLine;
                }
            } else {
                System.out.println("Command " + cmd + " failed, exit code is " + exitCode);

                while ((testLine = errorIs.readLine()) != null) {
                    System.out.println("testLine = " + testLine);
                }
            }
            is.close();
            errorIs.close();
            assert exitCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnStr;
    }

    public static String getResourceFilePath(String filePath){ 
        // Working with resoure files instead of the file system - OS Agnostic 
        String resourcePath = "";
        ClassLoader classLoader = ConjurTestHelper.class.getClassLoader();
        try {
            resourcePath = new File (classLoader.getResource(filePath).toURI()).getAbsolutePath();  
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resourcePath;
    }

    public static String readFile(String path) {
        StringBuilder result = new StringBuilder("");

        File file = new File(path);
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static void modifyFile(String filePath, String oldString, String newString) {
        // Because we are working with resource files, we are modifying in-memory files, 
        //     the files on disk are not modified. 
        File fileToBeModified = new File(filePath);
        String oldContent = "";
        BufferedReader reader = null;
        FileWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader(fileToBeModified));
            // Reading all the lines of input text file into oldContent
            String line = reader.readLine();
            while (line != null) {
                oldContent = oldContent + line + System.lineSeparator();
                line = reader.readLine();
            }
            // Replacing oldString with newString in the oldContent
            String newContent = oldContent.replaceAll(oldString, newString);
            // Rewriting the input text file with newContent
            writer = new FileWriter(fileToBeModified);
            writer.write(newContent);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Closing the resources
                reader.close();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static JSONObject getRequestParamsFromFile(String filePath) {
        JSONObject requestParams = new JSONObject();

        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();
         
        try (FileReader reader = new FileReader(filePath))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
 
            requestParams = (JSONObject) obj;
            //System.out.println(requestParams);
 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return requestParams;
    }

    public static JSONObject getRequestParams() {
        // must use intermediate parameterized HashMap to avoid warnings
        HashMap<String,Object> params = new HashMap<String,Object>();
        
        params.put("releaseTitle", "release from api");
        params.put("variables", new JSONObject());
        params.put("releaseVariables", new JSONObject());
        params.put("releasePasswordVariables", new JSONObject());
        params.put("scheduledStartDate", null);
        params.put("autoStart", false);
        JSONObject requestParams = new JSONObject(params);
        return requestParams;
    }

}