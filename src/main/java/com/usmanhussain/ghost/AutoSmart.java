package com.usmanhussain.ghost;

import com.usmanhussain.habanero.context.TestContext;
import com.usmanhussain.habanero.framework.StepDefs;
import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import org.apache.commons.io.FileUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AutoSmart extends StepDefs {

    private static String errorLogsFile = "Exceptions.xlsx";

    public AutoSmart(TestContext context) {
        super(context);
    }

    public static void main(String[] args) throws Throwable {
        File myFile = new File("target/Exceptions.xlsx");
        if (myFile.exists())
            myFile.delete();
        try {
            XSSFWorkbook myWorkBook = new XSSFWorkbook();
            XSSFSheet sheet = myWorkBook.createSheet();
            sheet.createRow(0);
            XSSFRow row = sheet.getRow(0);
            row.createCell(0).setCellValue("Feature");
            row.createCell(1).setCellValue("Scenario");
            row.createCell(2).setCellValue("Cucumber Exception");
            row.createCell(3).setCellValue("SureFire Exception");
            row.createCell(4).setCellValue("Har Json String");
            row.createCell(5).setCellValue("Probable Cause of failure");
            row.createCell(6).setCellValue("DOM");
            row.createCell(7).setCellValue("Probable Cause of failure2");
            String[] fileType = new String[]{"xml"};
            File cukeFilesDir = new File("target/cucumber_reports/regression_results");
            File sureFireFilesDir = new File("target/surefire-reports");
            File harFilesDir = new File("target/cucumber_reports");
            File HTMLFilesDir = new File("target/cucumber_reports");
            File jsonFilesDir = new File("target/cucumber_reports/regression_results");
            if (cukeFilesDir.exists() && sureFireFilesDir.exists() && harFilesDir.exists() && jsonFilesDir.exists()) {
                List<File> cukeFiles = (List<File>) FileUtils.listFiles(cukeFilesDir, fileType, false);
                List<File> sureFireFiles = (List<File>) FileUtils.listFiles(sureFireFilesDir, fileType, false);
                List<File> harFiles = (List<File>) FileUtils.listFiles(harFilesDir, new String[]{"har"}, false);
                List<File> HTMLFiles = (List<File>) FileUtils.listFiles(harFilesDir, new String[]{"html"}, false);
                List<File> jsonFiles = (List<File>) FileUtils.listFiles(jsonFilesDir, new String[]{"cucumber.json"}, false);
                ArrayList<ArrayList<String>> cucumberErrorString = getScenarioDetailsWithErrorMessage("cucumber", cukeFiles, "testcase");
                ArrayList<ArrayList<String>> sureErrorString = getScenarioDetailsWithErrorMessage("sureFire", sureFireFiles, "testcase");
                ArrayList<ArrayList<String>> harDetailsList = getHarDetails(harFiles);
                ArrayList<ArrayList<String>> failureCauseList = getErrorAnalysis(harFiles);
                ArrayList<ArrayList<String>> DOMfailureCauseList = getDOMErrorAnalysis(HTMLFiles, cucumberErrorString);
                ArrayList<ArrayList<String>> DOMString = getDOM(jsonFiles, sureErrorString);
                updateCucumberReport(failureCauseList);
                updateCucumberReport(DOMfailureCauseList);
                for (int i = 0; i < sureErrorString.size(); i++) {
                    if (sureErrorString.get(i).get(0).contains(cucumberErrorString.get(i).get(1))) {
                        sheet.createRow(i + 1);
                        row = sheet.getRow(i + 1);
                        row.createCell(0).setCellValue(cucumberErrorString.get(i).get(0));
                        row.createCell(1).setCellValue(cucumberErrorString.get(i).get(1));
                        row.createCell(2).setCellValue(cucumberErrorString.get(i).get(2));
                        row.createCell(3).setCellValue(sureErrorString.get(i).get(1));
                    } else {
                        for (ArrayList<String> cukeError : cucumberErrorString) {
                            if (sureErrorString.get(i).get(0).contains(cukeError.get(1))) {
                                sheet.createRow(i + 1);
                                row = sheet.getRow(i + 1);
                                row.createCell(0).setCellValue(cucumberErrorString.get(i).get(0));
                                row.createCell(1).setCellValue(cucumberErrorString.get(i).get(1));
                                row.createCell(2).setCellValue(cucumberErrorString.get(i).get(2));
                                row.createCell(3).setCellValue(sureErrorString.get(i).get(1));
                                break;
                            }
                        }
                    }
                    if (sureErrorString.get(i).get(0).toLowerCase().replaceAll("\\s+", "").contains(harDetailsList.get(i).get(0).toLowerCase())) {
                        row.createCell(4).setCellValue(harDetailsList.get(i).get(1));
                    } else {
                        for (ArrayList<String> harDetails : harDetailsList) {
                            if (sureErrorString.get(i).get(0).toLowerCase().replaceAll("\\s+", "").contains(harDetails.get(0).toLowerCase())) {
                                row.createCell(4).setCellValue(harDetailsList.get(i).get(1));
                            }
                        }
                    }
                    if (sureErrorString.get(i).get(0).toLowerCase().replaceAll("\\s+", "").contains(failureCauseList.get(i).get(0).toLowerCase())) {
                        row.createCell(5).setCellValue(failureCauseList.get(i).get(1));
                    } else {
                        for (ArrayList<String> failureDetails : failureCauseList) {
                            if (sureErrorString.get(i).get(0).toLowerCase().replaceAll("\\s+", "").contains(failureDetails.get(0).toLowerCase())) {
                                row.createCell(5).setCellValue(failureCauseList.get(i).get(1));
                            }
                        }
                    }
                    for (ArrayList<String> dom : DOMString) {
                        if (sureErrorString.get(i).get(0).toLowerCase().replaceAll("\\s+", "").contains(dom.get(0).toLowerCase().replaceAll("\\s+", ""))) {
                            row.createCell(6).setCellValue(dom.get(1));
                            break;
                        }
                    }
                    if (sureErrorString.get(i).get(0).toLowerCase().replaceAll("\\s+", "").contains(DOMfailureCauseList.get(i).get(0).toLowerCase())) {
                        row.createCell(7).setCellValue(DOMfailureCauseList.get(i).get(1));
                    } else {
                        for (ArrayList<String> failureDetails : DOMfailureCauseList) {
                            if (sureErrorString.get(i).get(0).toLowerCase().replaceAll("\\s+", "").contains(failureDetails.get(0).toLowerCase())) {
                                row.createCell(5).setCellValue(DOMfailureCauseList.get(i).get(1));
                            }
                        }
                    }
                }
                FileOutputStream os = new FileOutputStream(new File("target/" + errorLogsFile));
                myWorkBook.write(os);
                os.close();
            }
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public static ArrayList<ArrayList<String>> getScenarioDetailsWithErrorMessage(String plugin, List<File> xmlFiles, String node) throws Throwable {
        NodeList nodeList = null;
        ArrayList<ArrayList<String>> returnNodeList = new ArrayList<ArrayList<String>>();
        for (File xmlFile : xmlFiles) {
            String xml = new String(Files.readAllBytes(Paths.get(xmlFile.getPath())));
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                InputSource src = new InputSource();
                src.setCharacterStream(new StringReader(xml));
                doc = builder.parse(src);
            } catch (SAXException e) {
                e.printStackTrace();
                return returnNodeList;
            } catch (IOException e) {
                e.printStackTrace();
                return returnNodeList;
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                return returnNodeList;
            }
            nodeList = doc.getElementsByTagName(node);
            switch (plugin) {
                case "cucumber":
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        if (!nodeList.item(i).getTextContent().trim().isEmpty()) {
                            ArrayList<String> nodeDetails = new ArrayList<String>();
                            nodeDetails.add(nodeList.item(i).getAttributes().getNamedItem("classname").getNodeValue().trim());
                            nodeDetails.add(nodeList.item(i).getAttributes().getNamedItem("name").getNodeValue().trim());
                            nodeDetails.add(nodeList.item(i).getTextContent().trim());
                            returnNodeList.add(nodeDetails);
                        }
                    }
                    break;
                case "sureFire":
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        if (!nodeList.item(i).getTextContent().trim().isEmpty()) {
                            ArrayList<String> nodeDetails = new ArrayList<String>();
                            nodeDetails.add(nodeList.item(i).getAttributes().getNamedItem("name").getNodeValue().trim());
                            nodeDetails.add(nodeList.item(i).getTextContent().trim());
                            returnNodeList.add(nodeDetails);
                        }
                    }
                    break;
            }
        }
        return returnNodeList;
    }

    public static ArrayList<ArrayList<String>> getHarDetails(List<File> harFiles) throws Throwable {
        ArrayList<ArrayList<String>> returnString = new ArrayList<ArrayList<String>>();
        for (File harFile : harFiles) {
            ArrayList<String> result = new ArrayList<String>();
            String json = new String(Files.readAllBytes(Paths.get(harFile.getPath())));
            result.add(harFile.getName().split(".har")[0]);
            result.add(json);
            returnString.add(result);
        }
        return returnString;
    }

    public static ArrayList<ArrayList<String>> getDOM(List<File> jsonFiles, ArrayList<ArrayList<String>> sureErrorString) throws Throwable {
        ArrayList<ArrayList<String>> failedScenarioAndItsDOM = new ArrayList<ArrayList<String>>();
        String cucumberJson = FileUtils.readFileToString(new File("target/cucumber_reports/regression_results/cucumber.json"));
        JSONParser jp = new JSONParser();
        JSONArray parsedTargetJSON = (JSONArray) jp.parse(cucumberJson);
        JSONObject file = (JSONObject) parsedTargetJSON.get(0);
        JSONArray elements = (JSONArray) file.get("elements");
        for (ArrayList<String> errors : sureErrorString) {
            for (int i = 0; i < elements.size(); i++) {
                JSONObject scenario = (JSONObject) elements.get(i);
                String scenarioName = scenario.get("name").toString();
                if (errors.get(0).equalsIgnoreCase(scenarioName)) {
                    ArrayList<String> scenarioAndDOM = new ArrayList<>();
                    JSONArray after = (JSONArray) scenario.get("after");
                    JSONObject afterElement = (JSONObject) after.get(0);
                    JSONArray output = (JSONArray) afterElement.get("output");
                    scenarioAndDOM.add(scenarioName);
                    scenarioAndDOM.add((String) output.get(1));
                    failedScenarioAndItsDOM.add(scenarioAndDOM);
                }
            }
        }
        return failedScenarioAndItsDOM;
    }

    public static ArrayList<ArrayList<String>> getErrorAnalysis(List<File> harFiles) throws Throwable {
        ArrayList<ArrayList<String>> returnString = new ArrayList<ArrayList<String>>();
        for (File harFile : harFiles) {
            ArrayList<String> result = new ArrayList<String>();
            HarReader harReader = new HarReader();
            Har har = harReader.readFromFile(harFile);
            List<HarEntry> hEntry = har.getLog().getEntries();
            int hEntrySize = hEntry.size();
            if (hEntry.get(hEntrySize - 1).getResponse().getStatus() == 0 && hEntry.get(hEntrySize - 1).getRequest().getMethod().toString().equalsIgnoreCase("CONNECT")) {
                result.add(harFile.getName().split(".har")[0]);
                result.add("Network Connectivity issue: Connect method - Response code 0");
            } else {
                try (FileWriter fw = new FileWriter("target/response.txt", true);
                     BufferedWriter bw = new BufferedWriter(fw);
                     PrintWriter out = new PrintWriter(bw)) {
                    out.println(" ");
                    for (HarEntry entry : hEntry) {
                        if (entry.getResponse().getStatus() != 200) {
                            out.println("Response code " + entry.getResponse().getStatus() + "  : " + entry.getRequest().getUrl() + " ; ");
                        }
                    }
                } catch (Exception e) {
                }
                String content = new String(Files.readAllBytes(Paths.get("target/response.txt")));
                File myfile = new File("target/response.txt");
                if (myfile.exists())
                    myfile.delete();
                result.add(harFile.getName().split(".har")[0]);
                result.add(content);
            }
            returnString.add(result);
        }
        return returnString;
    }

    public static ArrayList<ArrayList<String>> getDOMErrorAnalysis(List<File> HTMLFiles, ArrayList<ArrayList<String>> cucumberErrorString) throws Throwable {
        ArrayList<ArrayList<String>> returnString = new ArrayList<ArrayList<String>>();
        for (File HTMLFile : HTMLFiles) {
            ArrayList<String> result = new ArrayList<String>();
            for (int i = 0; i < cucumberErrorString.size(); i++) {
                if (cucumberErrorString.get(i).get(1).replaceAll("\\s+", "").contains(HTMLFile.getName().split(".html")[0])) {
                    String domTypeAndValue[] = cucumberErrorString.get(i).get(2).split(":");
                    if (domTypeAndValue[1].contains("NoSuchElementException")) {
                        String domTypeAndValuesplit[] = domTypeAndValue[5].split("\"");
                        String domTypeAndValuesplit2[] = domTypeAndValue[6].split("\"");
                        if (domTypeAndValuesplit[1].equalsIgnoreCase("id")) {
                            if (findElementsInHTML(HTMLFile, "id", domTypeAndValuesplit2[1])) {
                                result.add(HTMLFile.getName().split(".html")[0]);
                                result.add("Id : " + domTypeAndValuesplit2[1] + " Found but Dom loading is taking longer than expected");
                            } else {
                                result.add(HTMLFile.getName().split(".html")[0]);
                                result.add("Id : " + domTypeAndValuesplit2[1] + " Not found in Loaded web page");
                            }
                        } else if (domTypeAndValuesplit[1].equalsIgnoreCase("css")) {
                            //TODO: Code need to be update
                        }
                    } else {
                        //For other than "NoSuchElementException" like "Unable to locate element"
                        result.add(HTMLFile.getName().split(".html")[0]);
                        result.add(" Need to be verified manually");
                    }
                }
            }
            returnString.add(result);
        }
        return returnString;
    }

    public static void updateCucumberReport(ArrayList<ArrayList<String>> failureList) {
        File jsonFile = new File("target/cucumber_reports/regression_results/cucumber.json");
        try {
            String cucumberJson = FileUtils.readFileToString(jsonFile);
            JSONParser jp = new JSONParser();
            JSONArray parsedTargetJSON = (JSONArray) jp.parse(cucumberJson);
            JSONArray elements = new JSONArray();
//            JSONObject file = (JSONObject) parsedTargetJSON.get(1);
//            JSONArray elements = (JSONArray) file.get("elements");

            for (int i = 0; i < parsedTargetJSON.size(); i++) {
                JSONObject file1 = (JSONObject) parsedTargetJSON.get(i);
                JSONArray elements1 = (JSONArray) file1.get("elements");
                for (int j = 0; j < elements1.size(); j++) {
                    elements.add(elements1.get(j));
                }
            }

            for (ArrayList<String> errors : failureList) {
                for (int i = 0; i < elements.size(); i++) {
                    JSONObject scenario = (JSONObject) elements.get(i);
                    String scenarioName = scenario.get("name").toString().replace(" ", "");
                    if (errors.get(0).equalsIgnoreCase(scenarioName)) {
                        System.out.println("==================================================" + scenarioName);
                        JSONArray steps = (JSONArray) scenario.get("steps");
                        JSONObject step = (JSONObject) steps.get(steps.size() - 1);
                        for (int j = 0; j < steps.size(); j++) {
                            step = (JSONObject) steps.get(j);
                            JSONObject result = (JSONObject) step.get("result");
                            String status = (String) result.get("status");
                            if (status.equals("failed")) {
                                step = (JSONObject) steps.get(j);
                                break;
                            }
                        }
                        JSONObject result = (JSONObject) step.get("result");
                        String errorMessage = result.get("error_message").toString();
                        result.put("error_message", errorMessage + "\n" + "possible cause of failure is " + errors.get(1));
                        errorMessage = result.get("error_message").toString();
                        break;
                    }
                }
            }
            FileWriter fileToWrite = new FileWriter("target/cucumber_reports/regression_results/cucumber.json");
            fileToWrite.write(parsedTargetJSON.toJSONString());
            fileToWrite.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static boolean findElementsInHTML(File location, String type, String name) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.parse(location, "UTF-8");
        try {
            switch (type) {
                case "id":
                    if (doc.getElementById(name).hasParent())
                        return true;
                case "css":
                    if (!(doc.select("#" + name).isEmpty()))
                        return true;
                default:
                    return false;
            }
        } catch (NullPointerException e) {
            return false;
        }
    }

}