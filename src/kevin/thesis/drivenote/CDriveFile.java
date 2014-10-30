package kevin.thesis.drivenote;

public class CDriveFile {
    	int SyncID;
    	String fileId;
    	String OwnByUser;
    	
    	public CDriveFile(){
    	}
    
    	public CDriveFile(int id,String fileid,String user){
    		this.SyncID=id;
    		this.fileId=fileid;
    		this.OwnByUser=user;
    	}
    	
    	public int getID(){
            return this.SyncID;
        }
     
        // setting id
        public void setID(int id){
            this.SyncID = id;
        }
     
        // getting name
        public String getFileId(){
            return this.fileId;
        }
     
        // setting name
        public void setFileId(String fileid){
            this.fileId=fileid;
        }
        
        public String getUser(){
            return this.OwnByUser;
        }
     
        // setting name
        public void setUser(String user){
            this.OwnByUser=user;
        }
}
