package googleapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;

public class SheetsAndJava {
	private static Sheets sheetsService;
	private static String APPLICATION_NAME = "Google Sheets Test";
	private static String SPREADSHEET_ID = "1cbHyYh4qADPxqs8-PdjzAjiM6x9icl_8-N4MfVSEbbA";
	
	private static Credential authorize() throws IOException, GeneralSecurityException {
		InputStream in = SheetsAndJava.class.getResourceAsStream("/credentials.json");
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), new InputStreamReader(in));
		List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);
		
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), 
				clientSecrets, scopes)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
				.setAccessType("online")
				.build();
		Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
		
		return credential;
	}
	
	public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
		Credential credential = authorize();
		return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), 
				JacksonFactory.getDefaultInstance(), credential)
				.setApplicationName(APPLICATION_NAME)
				.build();
				
	}
	
	public static String createNewGuild(String guildId, String guildName) throws IOException, GeneralSecurityException {
		sheetsService = getSheetsService();
		
		Spreadsheet spreadsheet = new Spreadsheet()
		        .setProperties(new SpreadsheetProperties()
		        .setTitle(guildName));
		spreadsheet = sheetsService.spreadsheets().create(spreadsheet)
		        .setFields("spreadsheetId")
		        .execute();
		return spreadsheet.getSpreadsheetId();
	}
	
	public static void createNewRaid(String guildId, String raidName) throws IOException, GeneralSecurityException {
		sheetsService = getSheetsService();
		
		AddSheetRequest addSheetRequest = new AddSheetRequest();
		List<Request> requestsList = new ArrayList<Request>();
		
		addSheetRequest.setProperties(new SheetProperties()
				.setTitle(raidName));
		
		Request request = new Request();
		request.setAddSheet(addSheetRequest);
		
		requestsList.add(request);
		sheetsService.spreadsheets().batchUpdate(guildId, new BatchUpdateSpreadsheetRequest().setRequests(requestsList));
	}
	
	public static void main(String[] args) throws IOException, GeneralSecurityException{
		 sheetsService = getSheetsService();
		 String range = "Core, Weekend 1!B7:B23";
		 
		 ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range)
				 .execute();
		 
		 List<List<Object>> values = response.getValues();
		 
		 if(values == null || values.isEmpty()) {
			 System.out.println("No data found");
		 }else {
			 for(List row: values) {
				 System.out.printf("%s\n", row.get(0));
			 }
		 }
		 
		 
	}
}
