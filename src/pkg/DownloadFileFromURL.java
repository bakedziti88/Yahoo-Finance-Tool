package pkg;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadFileFromURL {
	static String symbol = "vjet";
	static String mainURL = "https://finance.yahoo.com/quote/" + symbol;
	
	String finalCrumb = null;
	
    public static void main(String[] args) {
    	DownloadFileFromURL downldFileFrmUrl = new DownloadFileFromURL();
/*
        String url = "https://www.journaldev.com/sitemap.xml";
    	String nioDownloadFile = "C:/temp/sitemap.xml";
        String streamDownloadFile = "C:/temp/sitemap_stream.xml";
*/
        String url = "https://query1.finance.yahoo.com/v7/finance/download/SYMBOL?period1=1483333200&period2=1519163276&interval=1d&events=history&crumb="; //AtAGZ7yb/tK";
        String nioDownloadFile = "C:/temp/nio_" + downldFileFrmUrl.symbol + ".csv";
        String streamDownloadFile = "C:/temp/stream_" + downldFileFrmUrl.symbol + ".csv";
        url = url.replace("SYMBOL", symbol);
        try {
        	System.out.println("Downloading with downloadUsingStream");
            downldFileFrmUrl.downloadUsingStream(url, streamDownloadFile);
        	System.out.println("Downloading with downloadUsingNIO()");
            downldFileFrmUrl.downloadUsingNIO(url, nioDownloadFile);              
            System.out.println("All download is finished");            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadUsingNIO(String urlStr, String file) throws IOException {
        //URL url = new URL(urlStr);
        //ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        ReadableByteChannel rbc = Channels.newChannel(getUrlInputStream(urlStr));
        
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
        System.out.println(file + " download is finished");
    }

    public void downloadUsingStream(String urlStr, String file) throws IOException{ 
/*
    	// Use below URL to get the cookies and the crumb value to access the finance API
        String mainURL = "https://finance.yahoo.com/quote/" + symbol + "/history";
        Map<String, List<String>> cookies = setCookies(mainURL);	//will set finalCrumb
        // https://query1.finance.yahoo.com/v7/finance/download/IBM?period1=1467352800&period2=1498888800&interval=1d&events=history&crumb=2WsiR.p1KtI

        //set up cookies
        if ( cookies.get("set-cookie") != null ) {	// Crumb must match the cookie, otherwise will get Http 401 error
            for ( String c : cookies.get("set-cookie") ) 
                uc.setRequestProperty("Cookie", c);
        }
*/        
        //Seems to return 401...
        //InputStream in = uc.getInputStream();


        InputStream in = getUrlInputStream(urlStr);
        		
        BufferedInputStream bis = new BufferedInputStream(in);
        FileOutputStream fis = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int count=0;
        while((count = bis.read(buffer,0,1024)) != -1) {
            fis.write(buffer, 0, count);
        }
        fis.close();
        bis.close();
        System.out.println(file + " download is finished");
    }
    
    public InputStream getUrlInputStream(String urlStr) throws IOException{
    	InputStream inPutStr = null;
    	
    	// Use below URL to get the cookies and the crumb value to access the finance API       
        Map<String, List<String>> cookies = setCookies(mainURL);	//will set finalCrumb
        // https://query1.finance.yahoo.com/v7/finance/download/IBM?period1=1467352800&period2=1498888800&interval=1d&events=history&crumb=2WsiR.p1KtI

        // will need to append the crumb in the end to the below URL to have the actual crumb rather than the hardcoded one
        //String downloadUrl = "https://query1.finance.yahoo.com/v7/finance/download/" + symbol
        // + "?period1=1467352800&period2=1498888800&interval=1d&events=history&crumb=" + finalCrumb;
        URL url = new URL(urlStr + finalCrumb);        
        URLConnection uc = url.openConnection();
/*		
		From stackoverflow: doesn't seem to work for Yahoo Finance as of right now
        String userpass = "username:password";
        String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
        uc.setRequestProperty ("Authorization", basicAuth);
*/

        //set up cookies
        if ( cookies.get("set-cookie") != null ) {

        	// Crumb needs to match cookie, otherwise you get a nasty HTTP 401 error
            for ( String c : cookies.get("set-cookie") ) 
                uc.setRequestProperty("Cookie", c);
        }
        inPutStr = uc.getInputStream();
    	   	
    	return inPutStr;
    }
    
    // This method extracts the crumb and is being called from setCookies() method
    private String searchCrumb(URLConnection conn) throws IOException {
        String crumb = null;
        InputStream inStream = conn.getInputStream();
        InputStreamReader irdr = new InputStreamReader(inStream);
        BufferedReader rsv = new BufferedReader(irdr);

        Pattern crumbPattern = Pattern.compile(".*\"CrumbStore\":\\{\"crumb\":\"([^\"]+)\"\\}.*");

        String line = null;
        while (crumb == null && (line = rsv.readLine()) != null) {
            Matcher matcher = crumbPattern.matcher(line);
            if (matcher.matches())
                crumb = matcher.group(1);
        }
        rsv.close();
        System.out.println("Crumb is : "+crumb);
        return crumb;
    }


    // This method extracts the cookies from response headers and passes the same conn object to searchCrumb()
    // method to extract the crumb and set the crumb value in finalCrumb global variable
    public Map<String, List<String>> setCookies(String mainUrl) throws IOException {
    	System.out.println("mainUrl : " + mainUrl);
        // "https://finance.yahoo.com/quote/SPY";
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        URL url = new URL(mainUrl);
        URLConnection conn = url.openConnection();
        finalCrumb = searchCrumb(conn);
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
        	//find cookie
            if (entry.getKey() == null || !entry.getKey().equals("set-cookie"))
                continue;
            for (String s : entry.getValue()) {	// might not be necessary...
                map.put(entry.getKey(), entry.getValue());	//possible that there's more than 1 set-cookie???
                System.out.println(map);
            }
        }

        return map;
    }
}
