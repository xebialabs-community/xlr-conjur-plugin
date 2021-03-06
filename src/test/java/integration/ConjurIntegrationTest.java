package integration;
import static org.junit.Assert.assertTrue;

import java.io.File;
/**
 * Copyright 2019 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import java.io.IOException;
import java.util.Map;


import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import integration.util.ConjurTestHelper;

public class ConjurIntegrationTest {

    private static String ADMIN_KEY = "unknown";
    
    @ClassRule
    // This DockerComposeRule will clean up the docker containers after the tests have run
    public static DockerComposeContainer docker =
        new DockerComposeContainer(new File("build/resources/test/docker/docker-compose.yml"))
            .withLocalCompose(true);
    /*public static DockerComposeRule docker = DockerComposeRule.builder()
                                                .file(ConjurTestHelper.getResourceFilePath("docker/docker-compose.yml"))
                                                .pullOnStartup(true)
                                                .machine(getDockerMachine())
                                                .shutdownStrategy(getShutdownStrategy())
                                                .build();*/

    @BeforeClass
    public static void initialize() throws IOException, InterruptedException {
        // Start up and populate conjur with an account (quick-start), secrets and a host (xld/xld-01)
        // Place the password for host xld/xld-01 in a class variable so tests can access it.
        Map<String, String> conjurMap = ConjurTestHelper.initializeConjur();
        ADMIN_KEY = conjurMap.get("adminKey");
        
        // Put admin key in the conjur server config file
        // Import server config and template into xlr
        ConjurTestHelper.modifyFile((ConjurTestHelper.getResourceFilePath("docker/initialize/data/server-configs-Conjur.json")), "(\"password\": \")[A-Z,a-z,0-9]+(\")", 
            "\"password\": \""+ADMIN_KEY+"\"");
        ConjurTestHelper.initializeXLR();
    }    

    // Tests
    
    @Test
    public void testSecretRetrieval() throws Exception {
        String theResult = ConjurTestHelper.getConjurReleaseResult();
        assertTrue(theResult != null);
        assertTrue(ConjurTestHelper.readFile(ConjurTestHelper.getResourceFilePath("testExpected/secretRetrieval.txt")).equals(theResult));
        System.out.println("testSecretRetrieval passed ");
    }
}
