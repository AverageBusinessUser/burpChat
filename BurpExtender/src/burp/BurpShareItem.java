/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package burp;

import javax.xml.bind.DatatypeConverter;
import org.json.*;
/**
 *
 * @author cbellows
 */
public class BurpShareItem{
    
    //base items
    public String comment;
    public String highlight;
    public byte[] request;
    public byte[] response;
    
    //HttpServiceItems
    public String host;
    public int port;
    public String protocol;
    
    
    public BurpShareItem(){
        
    }
    
    public String getComment(){
        if(comment == null){
            return "";
        }
        return comment;
    }
    
    
    public void loadFromRequestResponse(IHttpRequestResponse reqres){
        comment = reqres.getComment();
        highlight = reqres.getHighlight();
        request = reqres.getRequest();
        response = reqres.getResponse();
        host = reqres.getHttpService().getHost();
        port = reqres.getHttpService().getPort();
        protocol = reqres.getHttpService().getProtocol();
    }
    
   
    
    public boolean loadJson(String input){
        
        //we have to have the following, if they do not exist, bail
        try{
            //load the input string
            JSONObject json = new JSONObject(input);
            host = json.getString("host");
            port = json.getInt("port");
            protocol = json.getString("protocol");
            request = DatatypeConverter.parseBase64Binary(json.getString("request"));
            
            //these are all optional
            if(json.has("comment"))
                comment = new String(DatatypeConverter.parseBase64Binary(json.getString("comment")));
            if(json.has("highlight"))
                highlight = new String(DatatypeConverter.parseBase64Binary(json.getString("highlight")));
            if(json.has("response"))
                response = DatatypeConverter.parseBase64Binary(json.getString("response"));
            
            }catch(Exception ex){
                return false;
            }
        return true;
        
    }
    
    
    public String toJson(){
        JSONObject json = new JSONObject();
        
        try{
        //we need to check that our values are not null, otherwise the put fails
        if(comment!=null)
            json.put("comment", DatatypeConverter.printBase64Binary(comment.getBytes()));
        if(highlight!=null)
            json.put("highlight", DatatypeConverter.printBase64Binary(highlight.getBytes()));
        if(response!=null)
            json.put("response", DatatypeConverter.printBase64Binary(response));
        
        //we have to have these items
        json.put("host", host);
        json.put("port", port);
        json.put("protocol", protocol);
        json.put("request", DatatypeConverter.printBase64Binary(request));
        }catch(Exception ex){
            return ex.toString();
        }

        
        return json.toString();
    }
    
    
    
    @Override
    public String toString(){
        String out = "";
        
        out+= "Host: "+host+"\n";
        out+= "Port: "+port+"\n";
        out+= "Request: "+new String(request)+"\n";

        
        return out;
    }
  
    
}
