package kevin.thesis.drivenote;

public class CLocalFile {
    	int SyncID;
    	String filepath;
    	String OwnByUser;
    	
    	public CLocalFile(){
    	}
    
    	public CLocalFile(int id,String fp,String user){
    		this.SyncID=id;
    		this.filepath=fp;
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
        public String getFilePath(){
            return this.filepath;
        }
     
        // setting name
        public void setFilePath(String fp){
            this.filepath=fp;
        }
        
        public String getUser(){
            return this.OwnByUser;
        }
     
        // setting name
        public void setUser(String user){
            this.OwnByUser=user;
        }
}
