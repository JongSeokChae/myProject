package com.sandc.enterprise.restapi;

import com.sandc.enterprise.classifier.CaseDetails;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.json.JSONException;

public class SalesforceConnect {

  static final String USERNAME     = "felix.chae@sandc.com.asp2017";
  static final String PASSWORD     = "Abcdef!2N4pssXFPAvXCtQscukTBLEBrT";
  static final String LOGINURL     = "https://cs9.salesforce.com";
  static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
  static final String CLIENTID     = "3MVG9FS3IyroMOh5X4jsCqFxMjp7KTA0FbODdqpiDUCEAuN0Iv6w6kjXdb30WjjEz1nsZvxWpWU77ayRuhz.S";
  static final String CLIENTSECRET = "2912406664135332708";

  private static String REST_ENDPOINT = "/services/data" ;
  private static String API_VERSION = "/v39.0" ;
  private static String baseUri;
  private static Header oauthHeader;
  private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
  private static HttpPost httpPost;
  private static String Status;

  /**
   * Login to SalesForce by Connecting REST API to SalesForce
   */
  public void login() {

    HttpClient httpclient = HttpClientBuilder.create().build();

    // Assemble the login request URL
    String loginURL = LOGINURL +
        GRANTSERVICE +
        "&client_id=" + CLIENTID +
        "&client_secret=" + CLIENTSECRET +
        "&username=" + USERNAME +
        "&password=" + PASSWORD;
    // Login requests must be POSTs
    httpPost = new HttpPost(loginURL);
    HttpResponse response = null;

    try {
      // Execute the login POST request
      response = httpclient.execute(httpPost);
    } catch (ClientProtocolException cpException) {
      cpException.printStackTrace();
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }

    // verify response is HTTP OK
    final int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != HttpStatus.SC_OK) {
      System.out.println("Error authenticating to Force.com: "+statusCode);
      // Error is in EntityUtils.toString(response.getEntity())
      return;
    }

    String getResult = null;
    try {
      getResult = EntityUtils.toString(response.getEntity());
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }

    JSONObject jsonObject = null;
    String loginAccessToken = null;
    String loginInstanceUrl = null;

    try {
      jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
      loginAccessToken = jsonObject.getString("access_token");
      loginInstanceUrl = jsonObject.getString("instance_url");
    } catch (JSONException jsonException) {
      jsonException.printStackTrace();
    }

    baseUri = loginInstanceUrl + REST_ENDPOINT + API_VERSION ;
    oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken) ;
    System.out.println("oauthHeader1: " + oauthHeader);
    System.out.println("\n" + response.getStatusLine());
    System.out.println("Successful login");
    System.out.println("instance URL: "+loginInstanceUrl);
    System.out.println("access token/session ID: "+loginAccessToken);
    System.out.println("baseUri: "+ baseUri);

  }

  /**
   * Logout from SalesForce
   */
  public void logOut() {
    // release connection
    httpPost.releaseConnection();
  }

  /**
   * Create a new case or update the existing case. If a case number is provided and a case with that number does not
   * exists, return a error message. If it is an existing case and not resolved (open), update the fields on the case.
   * If the case has been resolved (closed), then create a new case with relevant details from the old case and
   * create a new case and return the new case number.
   * @param caseDetails a collection of values of fields
   * @return a list with caseNumber and caseId
   */
  public ArrayList<String> createOrUpdateCases(CaseDetails caseDetails) {
    String caseNum = caseDetails.getCaseNum();
    String caseId = caseDetails.getCaseID();
    try {
      // Not providing a CaseNumber field means the caller just wants to create a new case
      if (caseNum == null) return createCases(caseDetails);

      // If a case does not exist return a error message
      if (!queryCases(caseNum)) {
        System.out.println("Case with a case number " + caseNum + " does not exist.");
        return null;
      }
      // If a case exists and...
      // 1. if a case is resolved, CREATE a new case
      if (Status.equalsIgnoreCase("Closed")) {
        return createCases(caseDetails);
      }
      // 2. if a case is open, UPDATE the fields on the case.
      else if(!Status.equalsIgnoreCase("Closed")) {
        updateCase(caseDetails);
        ArrayList<String> caseNumcaseId = new ArrayList<>();
        caseNumcaseId.add(caseNum);
        caseNumcaseId.add(caseId);
        return caseNumcaseId;

      }
    } catch (JSONException e) {
      System.out.println("Issue creating JSON or processing results");
      e.printStackTrace();
    }
    catch (NullPointerException npe) {
      npe.printStackTrace();
    }
    return null;
  }

  /**
   * Create a case with values of fields described in CASEDETAILS
   * @param caseDetails a collection of values of fields
   * @return a list with caseNumber and caseId
   */
  public ArrayList<String> createCases(CaseDetails caseDetails) {
    login();
    System.out.println("\n_______________  INSERT NEW CASE  _______________");
    String uri = baseUri + "/sobjects/Case/";
    String caseNum = null;
    String caseId = null;
    try {

      //create the JSON object containing the new Case details.
      JSONObject sfcase = new JSONObject();
      sfcase.put("AccountID", "0018000000UUdNaAAL");
      sfcase.put("Subject", caseDetails.getSubject());
      sfcase.put("Type", caseDetails.getType());
      sfcase.put("Sub_Type__c", caseDetails.getProblemSubType());
      sfcase.put("Product_Group__c", caseDetails.getProductGroup());
      sfcase.put("Product_Group_Sub_Type__c", caseDetails.getProductGroupSubType());
      sfcase.put("Description", caseDetails.getDescription());

      System.out.println("JSON for case record to be inserted:\n" + sfcase.toString(1));

      //Construct the objects needed for the request
      HttpClient httpClient = HttpClientBuilder.create().build();

      HttpPost httpPost = new HttpPost(uri);
      httpPost.addHeader(oauthHeader);
      httpPost.addHeader(prettyPrintHeader);
      // The message we are going to post
      StringEntity body = new StringEntity(sfcase.toString(1));
      body.setContentType("application/json");
      httpPost.setEntity(body);

      //Make the request
      HttpResponse response = httpClient.execute(httpPost);

      //Process the results
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 201) {
        String response_string = EntityUtils.toString(response.getEntity());
        JSONObject json = new JSONObject(response_string);
        // Store the retrieved case id to use when we update the Case.
        caseId = json.getString("id");
        caseDetails.setCaseID(caseId);
        caseNum = fetchCaseNum(caseDetails);
        caseDetails.setCaseNum(caseNum);
      } else {
        System.out.println("Insertion unsuccessful. Status code returned is " + statusCode);
      }
    } catch (Exception e) {
      System.out.println("Error: " + e);
    }

    createTag(caseDetails);

    ArrayList<String> caseNumcaseId = new ArrayList<>();
    caseNumcaseId.add(caseNum);
    caseNumcaseId.add(caseId);
    System.out.println(caseNumcaseId);

    logOut();
    return caseNumcaseId;
  }

  /**
   * Create a tag attached to a new case inserted into a SalesForce database
   * @param caseDetails a collection of values of fields
   */
  public void createTag(CaseDetails caseDetails) {
    System.out.println("\n_______________  CREATE TAGS _______________");
    String uri = baseUri + "/sobjects/CaseTag/";
    try {

      //create the JSON object containing the new Case details.
      JSONObject sfcaseTag = new JSONObject();
      List<String> multipleTags = caseDetails.getTags();
      sfcaseTag.put("Name", multipleTags.get(multipleTags.size()-1));
      sfcaseTag.put("ItemId", caseDetails.getCaseID());
      sfcaseTag.put("Type", "Public");

      System.out.println("JSON for case record to be inserted:\n" + sfcaseTag.toString(1));

      //Construct the objects needed for the request
      HttpClient httpClient = HttpClientBuilder.create().build();

      HttpPost httpPost = new HttpPost(uri);
      httpPost.addHeader(oauthHeader);
      httpPost.addHeader(prettyPrintHeader);
      // The message we are going to post
      StringEntity body = new StringEntity(sfcaseTag.toString(1));
      body.setContentType("application/json");
      httpPost.setEntity(body);

      //Make the request
      HttpResponse response = httpClient.execute(httpPost);

      //Process the results
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 201) {
        String response_string = EntityUtils.toString(response.getEntity());
        JSONObject json = new JSONObject(response_string);
        // Store the retrieved Case id to use when we update the Case.
      } else {
        System.out.println("Tag creation unsuccessful. Status code returned is " + statusCode);
      }
    } catch (JSONException e) {
      System.out.println("Issue creating JSON or processing results");
      e.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (NullPointerException npe) {
      npe.printStackTrace();
    }
  }

  /**
   * Overwrites values of fields of a case with a given CaseID
   * @param caseDetails a collection of values of fields
   */
  public void updateCase(CaseDetails caseDetails) {
    login();
    System.out.println("\n_______________ UPDATE CASE _______________");

    //Notice, the id for the record to update is part of the URI, not part of the JSON
    String uri = baseUri + "/sobjects/Case/" + caseDetails.getCaseID();
    try {
      //Create the JSON object containing the updated Case last name
      //and the id of the Case we are updating.
      JSONObject sfcase = new JSONObject();
      sfcase.put("AccountID", "0018000000UUdNaAAL");
      sfcase.put("Subject", caseDetails.getSubject());
      sfcase.put("Type", caseDetails.getType());
      sfcase.put("Sub_Type__c", caseDetails.getProblemSubType());
      sfcase.put("Product_Group__c", caseDetails.getProductGroup());
      sfcase.put("Description", caseDetails.getDescription());
      System.out.println("JSON for update of case record:\n" + sfcase.toString(1));

      //Set up the objects necessary to make the request.
      //DefaultHttpClient httpClient = new DefaultHttpClient();
      HttpClient httpClient = HttpClientBuilder.create().build();

      HttpPatch httpPatch = new HttpPatch(uri);
      httpPatch.addHeader(oauthHeader);
      httpPatch.addHeader(prettyPrintHeader);
      StringEntity body = new StringEntity(sfcase.toString(1));
      body.setContentType("application/json");
      httpPatch.setEntity(body);

      //Make the request
      HttpResponse response = httpClient.execute(httpPatch);

      //Process the response
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 204) {
        System.out.println("Updated the Case successfully.");
      } else {
        System.out.println("Case update NOT successfully. Status code is " + statusCode);
      }
    } catch (JSONException e) {
      System.out.println("Issue creating JSON or processing results");
      e.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (NullPointerException npe) {
      npe.printStackTrace();
    }
    logOut();
  }

  /**
   * Check if a case with CASENUM exists
   * @param caseNum CaseNumber of the case we want to fetch from the SalesForce database
   * @return true if a case with CASENUM exists
   */
  public boolean queryCases(String caseNum) {
    login();
    System.out.println("\n_______________ QUERY CASES _______________");
    try {

      //Set up the HTTP objects needed to make the request.
      HttpClient httpClient = HttpClientBuilder.create().build();

      String uri = baseUri + "/query?q=Select"
          + "+caseNumber+,+Status+From+Case+Where+caseNumber=" + "'" + caseNum + "'";
      System.out.println("Query URL: " + uri);
      HttpGet httpGet = new HttpGet(uri);
      System.out.println("oauthHeader2: " + oauthHeader);
      httpGet.addHeader(oauthHeader);
      httpGet.addHeader(prettyPrintHeader);

      // Make the request.
      HttpResponse response = httpClient.execute(httpGet);

      // Process the result
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 200) {
        String response_string = EntityUtils.toString(response.getEntity());
        try {
          JSONObject json = new JSONObject(response_string);
          System.out.println("JSON result of Query:\n" + json.toString(1));
          JSONArray j = json.getJSONArray("records");
          if (j == null || j.length() == 0) {
            return false;
          }
          else {
            for (int i = 0; i < j.length(); i++) {
              Status = json.getJSONArray("records").getJSONObject(i).getString("Status");
            }
            return true;
          }
        } catch (JSONException je) {
          je.printStackTrace();
        }
      } else {
        System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
        System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
        System.out.println(getBody(response.getEntity().getContent()));
        System.exit(-1);
        return false;
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (NullPointerException npe) {
      npe.printStackTrace();
    }
    logOut();
    return false;
  }

  /**
   * Fetch a CaseNumber of a case whose values of fields are matched with ones in CASEDETAILS
   * @param caseDetails a collection of values of fields
   * @return a CaseNumber
   */
  public String fetchCaseNum(CaseDetails caseDetails) {
    login();
    System.out.println("\n_______________ FETCH CaseNumber _______________");
    try {

      //Set up the HTTP objects needed to make the request.
      HttpClient httpClient = HttpClientBuilder.create().build();

      String uri = baseUri + "/query?q=Select"
          + "+Subject+,+Type+,+caseNumber+,+Status+"
          + "From+Case+"
          + "Where+Id=" + "'" + caseDetails.getCaseID() + "'";


      System.out.println("Query URL: " + uri);
      HttpGet httpGet = new HttpGet(uri);
      System.out.println("oauthHeader2: " + oauthHeader);
      httpGet.addHeader(oauthHeader);
      httpGet.addHeader(prettyPrintHeader);

      // Make the request.
      HttpResponse response = httpClient.execute(httpGet);

      // Process the result
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 200) {
        String response_string = EntityUtils.toString(response.getEntity());
        try {
          JSONObject json = new JSONObject(response_string);
          System.out.println("JSON result of Query:\n" + json.toString(1));
          JSONArray j = json.getJSONArray("records");
          if (j == null || j.length() == 0) {
            return null;
          }
          for (int i = 0; i < j.length(); i++){
            return json.getJSONArray("records").getJSONObject(i).getString("CaseNumber");
          }
        } catch (JSONException je) {
          je.printStackTrace();
        }
      } else {
        System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
        System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
        System.out.println(getBody(response.getEntity().getContent()));
        System.exit(-1);
        return null;
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (NullPointerException npe) {
      npe.printStackTrace();
    }
    logOut();
    return null;
  }

  /**
   *
   * @param inputStream
   * @return
   */
  private static String getBody(InputStream inputStream) {
    String result = "";
    try {
      BufferedReader in = new BufferedReader(
          new InputStreamReader(inputStream)
      );
      String inputLine;
      while ( (inputLine = in.readLine() ) != null ) {
        result += inputLine;
        result += "\n";
      }
      in.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    return result;
  }

  public static void main(String[] args) {
    SalesforceConnect sfc = new SalesforceConnect();
    //String subject = "Switchgear parts for"; //some invalid subject format: "a&b", "a - b"
    String subject = "Trip Saver II Repair";
    String description = "Thomas loaded his Software on his Panasonic Laptop, it is running WIndow's 7. ";
    ArrayList<String> tags = new ArrayList<>(Arrays.asList("TripSaver2", "TripSaverII", "TripSaver", "Power System", "Recurring overloads" ));
    String type = "Issue";
    String problemSubType = "Product";
    String productGroup = "Automation Products";
    String productGroupSubtype = "TripSaver Dropout Recloser";
    //String description = " The ARC compressors on all 5 units are cracked and need replacing.";
    //ArrayList<String> tags = new ArrayList<>(Arrays.asList("TripSaver2", "TripSaverII", "TripSaver"));

    //String caseNum = "00223651";
    //String caseId = "500K000000ExMeQIAV";

    //[00223651, 500K000000ExMeQIAV]
    CaseDetails caseDetails = new CaseDetails(subject, description);
    caseDetails.setType(type);
    caseDetails.setProblemSubType(problemSubType);
    caseDetails.setProductGroup(productGroup);
    caseDetails.setProductGroupSubType(productGroupSubtype);
    caseDetails.setTags(tags);

    //caseDetails.setCaseNum(caseNum);
    //caseDetails.setCaseID(caseId);

    sfc.createOrUpdateCases(caseDetails);

  }


}