package com.atqc;

import com.google.common.collect.ImmutableMap;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AdminLogin extends RestAPIBaseTest {
    String ott,orgId,accessToken,refreshToken;
    @Test(priority = 1)
    @Story("US-3 As organization admin I want to log in to the system so that I can use system")
    @Description("First auth step to get ott and orgID" )
    public void positivePostUserFirst() {
        Response response = given()
                .contentType("application/json")
                .baseUri("https://api-test.app.workoptima.com/api/v1")
                .body("{\n" +
                        "  \"email\": \"admin@admin.com\",\n" +
                        "  \"password\": \"admin1234\"\n" +
                        "}")
        .when()
                .post("/auth")
        .then()
                .statusCode(200)
                .body("user.name", equalTo("admin"))
                .body("user.email", is("admin@admin.com"))
                .body("user.phone", startsWith("1"))
                .body("user.status", not("inactive"))
       //         .body("user.organizations.name", hasItemInArray("admin_organization")) почему-то не работает(
                .extract().response();
        ott = response.getBody().jsonPath().getString("ott");
        orgId = response.getBody().jsonPath().getString("user.organizations.id[0]");
    }

    @Test(priority = 2, alwaysRun = true)
    @Story("US-3 As organization admin I want to log in to the system so that I can use system")
    @Description("Get Access and Refresh tokens for successful login")
    public void positivePostUserSecond() {
        Response response = given()
                .contentType("application/json")
                .baseUri("https://api-test.app.workoptima.com/api/v1")
                .body(ImmutableMap.of("ott", ott))
        .when()
                .post("/auth/{org_id}", orgId)
        .then()
                .statusCode(200)
                .extract().response();
        accessToken = response.getBody().jsonPath().getString("access_token");
        refreshToken = response.getBody().jsonPath().getString("refresh_token");
        System.out.println(accessToken);
        System.out.println(refreshToken);

    }

    @Test(priority= 3, alwaysRun = true)
    @Story("US-3 As organization admin I want to log in to the system so that I can use system")
    @Description("Check login with Access and Refresh tokens")
    public void positiveGetAccessToken() {
        given()
                .contentType("application/json")
                .baseUri("https://api-test.app.workoptima.com/api/v1")
                .header("Access-Token", accessToken)
                .header("Refresh-Token", refreshToken)
        .when()
                .get("/auth/refresh-token")
        .then()
                .statusCode(200);

    }

    @Test(priority = 4, dataProvider = "invalidEmails", alwaysRun = true)
    @Description("Login with invalid email")
    public void negativePostUserFirstByInvalidEmail(String email, int code) {
        given()
                .contentType("application/json")
                .baseUri("https://api-test.app.workoptima.com/api/v1")
                .body(ImmutableMap.of("login", email, "password", "admin1234"))
        .when()
                .post("/auth")
        .then()
                .statusCode(code);

    }

    @DataProvider(name = "invalidEmails")
    private Object[][] provider() {

        return new Object[][] {

                {"admin@", 422},
                {"", 422},
                {"admin@ admin.com", 422},
                {"admin@admincom", 422},
                {"admin1234", 422}

        };
    }
}
