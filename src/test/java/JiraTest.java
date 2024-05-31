import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.path.json.JsonPath;
import org.testng.Assert;

import java.io.File;

import static io.restassured.RestAssured.given;

public class JiraTest {
    public static void main(String[] args) {
        RestAssured.baseURI= "http://localhost:8080/";
//        Create login sesion
        SessionFilter session= new SessionFilter();
        given().relaxedHTTPSValidation().header("Content-Type","application/json")
                .body("{ \"username\": \"akashniche\", \"password\": \"Gargi@4321\" }")
                .filter(session)
        .when()
                .post("rest/auth/1/session")
        .then().log().all();
//        Add comment
        String comment= "This is a comment for test purpose";
       String addCommentResponse= given().
                pathParam("id","10101")
                .header("Content-Type","application/json")
                .body("{\n" +
                        "    \"body\": \""+comment+"\",\n" +
                        "    \"visibility\": {\n" +
                        "        \"type\": \"role\",\n" +
                        "        \"value\": \"Administrators\"\n" +
                        "    }\n" +
                        "}")
                .filter(session)
        .when().post("rest/api/2/issue/{id}/comment")
        .then().statusCode(201).log().all().extract().response().asString();

        JsonPath js= new JsonPath(addCommentResponse);
        int commentId= js.getInt("id");
//        Add attachments
        given()
                .header("X-Atlassian-Token","no-check")
                .header("Content-Type","multipart/form-data")
                .pathParam("id","10101")
                .multiPart("file",new File("Jira_attachment.txt"))
                .filter(session)
                .when()
                .post("rest/api/2/issue/{id}/attachments")
                .then()
                .log().all()
                .statusCode(200);
//        Get Issue
        String getIssueResponse= given()
                .filter(session)
                .pathParam("id","10101")
                .queryParam("fields","comment")
                .when()
                .get("rest/api/2/issue/{id}")
                .then()
                .log().all()
                .extract().response().asString();
        js= new JsonPath(getIssueResponse);
        int commentCount= js.getInt("fields.comment.comments.size()");
        for(int i=0;i<commentCount;i++){
//            System.out.println((String) js.get("fields.comment.comments["+i+"].id"));
            int commentIssue= js.getInt("fields.comment.comments["+i+"].id");
            if(commentId==commentIssue){
                String message= js.get("fields.comment.comments["+i+"].body");
                Assert.assertEquals(comment,message);
                break;
            }
        }

    }
}
