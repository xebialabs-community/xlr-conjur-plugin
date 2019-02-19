/**
 * Copyright 2018 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.Maps;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.*;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.waiting.*;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import testUtil.*;

public class ConjurIntegrationTest {

    private static String HOST_KEY = "unknown";
    private static String ADMIN_KEY = "unknown";
    private static String pathPrefix = "src/test/resources/docker/";

    @ClassRule
    // This DockerComposeRule will clean up the docker containers after the tests have run
    public static DockerComposeRule docker = DockerComposeRule.builder()
                                                .file(getResourceFilePath("docker/docker-compose.yml"))
                                                .pullOnStartup(true)
                                                .machine(getDockerMachine())
                                                //.shutdownStrategy(ShutdownStrategy.SKIP)
                                                .build();

    @BeforeClass
    public static void initialize() throws IOException, InterruptedException {
        // Start up and populate conjur with an account (quick-start), secrets and a host (xld/xld-01)
        // Place the password for host xld/xld-01 in a class variable so tests can access it.
        Map<String, String> conjurMap = ConjurTestHelper.initializeConjur();
        ADMIN_KEY = conjurMap.get("adminKey");
        HOST_KEY = conjurMap.get("hostKey");
        
        System.out.println("The admin key is - "+ADMIN_KEY);
        // Put admin key in the conjur server config file
        // Import server config and template into xlr
        ConjurTestHelper.modifyFile((getResourceFilePath("docker/initialize/data/server-configs-Conjur.json")), "(\"password\": \")[A-Z,a-z,0-9]+(\")", 
            "\"password\": \""+ADMIN_KEY+"\"");
        ConjurTestHelper.initializeXLR();
    }

    // Utility Methods

    private static DockerMachine getDockerMachine() {
        // We need to perform a one time run on the conjur server docker container to generate and capture the conjur data key
        // The key is then placed in the docker environment so that conjur can then be started and populated with secrets
        String key = ConjurTestHelper.getCommandResult(
                "docker-compose -f "+ getResourceFilePath("docker/docker-compose.yml")+" run --no-deps --rm conjur data-key generate");
        DockerMachine dockerMachine = DockerMachine.localMachine()
                .withAdditionalEnvironmentVariable("CONJUR_DATA_KEY", key).build();
        return dockerMachine;
    }

    private static String getResourceFilePath(String filePath){  
        ClassLoader classLoader = ConjurIntegrationTest.class.getClassLoader();
        String resourcePath = classLoader.getResource(filePath).getFile();
        return resourcePath;
    }

    // Tests

    @Test
    public void testSecretRetrieval() throws Exception {
        String theResult = ConjurTestHelper.getConjurReleaseResult();
        assertTrue(theResult != null);
        assertTrue(ConjurTestHelper.readFile(getResourceFilePath("testExpected/secretRetrieval.txt")).equals(theResult));
        System.out.println("testSecretRetrieval passed ");
    }
}
