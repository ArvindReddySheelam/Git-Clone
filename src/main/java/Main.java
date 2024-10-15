import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Main {

/* 
Command-Line Arguments: The String[] args parameter allows you to pass command-line arguments to your Java program, 
When you run a Java application from the command line, you can provide input values directly to the program.

Flexibility: This feature provides flexibility in how you run your program, 
Instead of hardcoding values, you can specify them at runtime,
This is particularly useful for configuration settings, file names, or other parameters that might change.

*/
  public static void main(String[] args) throws IOException{
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    if(args.length < 1){
      System.err.println("Please provide a single argument.");
      System.exit(0);
    }
    // Uncomment this block to pass the first stage
    // args is an array of String objects. Each element in the array corresponds to a command-line argument passed to the program.
    final String command = args.length > 0 ? args[0] : "";

    
    // switch statement, which evaluates the value of command to determine which block of code to execute.
    switch (command) {
      case "init" -> {
        //  Creates a File object representing a directory named .git, which is typically used in Git version control systems.
        final File root = new File(".git");
         // Creates a new directory named "objects" inside the .git directory.
        File new_subDir = new File(root, "objects");
        if (new_subDir.exists()) {
          System.out.println("Directory already exists.");
      } else if (new_subDir.mkdirs()) {
          System.out.println("New directory created successfully.");
      } else {
          System.out.println("Failed to create new directory.");
      }

        File new_subDir2 = new File(root, "refs");
        if (new_subDir2.exists()) {
          System.out.println("Directory already exists.");
      } else if (new_subDir2.mkdirs()) {
          System.out.println("New directory created successfully.");
      } else {
          System.out.println("Failed to create new directory.");
      }

        final File head = new File(root, "HEAD");
    
        try {
          head.createNewFile();
          Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
          System.out.println("Initialized git directory");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      
      case "cat-file"  -> {
        if(args.length  != 3 || !args[1].equals("-p")){
          System.out.println("Usage: cat-file -p <object_hash>");
          System.exit(1);
        }
        //  This is a placeholder for the cat-file command, which is used to display the contents
        //  of a file in the Git repository.
        String hasOf_gitObject = args[2];
        // Git objects are stored in the .git/objects directory. The path to an object is derived from its hash.
        /* 
        This extracts the first two characters of the hash. 
        In Git's object storage, the first two characters of an object's hash represent the directory in which the file is stored.
        */
        String dir = hasOf_gitObject.substring(0, 2);
        // Now, extracting the complete path of the file with ramaining 38-characters
        String object_file = hasOf_gitObject.substring(2);
        // The path to the object file is .git/objects/<dir>/<object_file>
        File blob_File = new File(".git/objects/" + dir + "/" +  object_file);
        // The cat-file command is used to display the contents of a file in the Git repository.
        
        //  ---try-with-resource-statement for automatic  resource management---
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(blob_File))))) {

          // Read the contents of the file
          StringBuilder sbr = new StringBuilder();
          String file_content;

          while((file_content = br.readLine()) != null){
            if(sbr.length() > 0 ){
              sbr.append(System.lineSeparator());
            }
            sbr.append(file_content);
          }
          // File format -> blob <size>\0<content>
          String data =  sbr.toString();
          int null_byte = data.indexOf('\0');
          if(null_byte != -1){
             data = data.substring(null_byte + 1);
          }
          System.out.print(data);
        }
        catch(IOException e){
          throw new RuntimeException(e);
        }
      }
      
      case "hash-object" -> hash_Object(args);
      case "ls-tree" -> ls_Tree(args);
      case "write-tree" -> writeTreeObject();
      case "commit-tree" -> commitTree(args);
      default -> System.out.println("Unknown command: " + command);
    }
    
  }
   
  private static void hash_Object(String[] args) throws IOException{
    //  This is a placeholder for the hash-object command, which is used to compute the hash
    //hash-object -w <file_name> three arguments expected
    if(args.length < 2){
      throw new IllegalArgumentException("Usage: java main hash-object -w <file_name>");
    }
    //  The hash-object command is used to compute the hash of a file.
    //  The -w option is used to write the hash to the index(.git/Object) directory. 
    boolean write = args[1].equals("-w");
    String file_Name = write  ? args[2] : args[1];
    String  hash = computeHash(file_Name,write);
    System.out.println(hash);
  }

  private static String computeHash(String file_Name,boolean  write) throws IOException{
    
    String sha1Hash  = null;

   //InputStream that you can use to read the contents of the file as a stream of bytes.
    try(InputStream byte_Data = Files.newInputStream(Paths.get(file_Name))){
      
      long file_Size =  Files.size(Paths.get(file_Name));

      //file-format :  blob <size>\0<content>
      String header = "blob" + file_Size + "\0";
      byte[] header_bytes =  header.getBytes(StandardCharsets.UTF_8);
      byte[] fil_bytes = byte_Data.readAllBytes();

      //combined
      byte[] combined  = new byte[header_bytes.length + fil_bytes.length];
      System.arraycopy(header_bytes,0,combined,0,header_bytes.length);
      System.arraycopy(fil_bytes,0,combined,header_bytes.length,fil_bytes.length);
      
      sha1Hash = shaHex(combined);
      // write to .git/object directory
      if(write){
        write_to_GitObject(sha1Hash,combined);
      }
    }
    catch(IOException ioe){
      ioe.printStackTrace();
    }
    return sha1Hash;
  }

  private static void write_to_GitObject(String sha_1, byte[] combined) throws IOException {
    Path Object_Path = Paths.get(get_path(sha_1));
    
    Files.createDirectories(Object_Path.getParent());

    try(OutputStream os =  Files.newOutputStream(Object_Path)){

      DeflaterOutputStream dos = new DeflaterOutputStream(os);
      dos.write(combined);
      //close  the stream
       dos.close();
    }
  }

  private static  String get_path(String sha_1) {
    return ".git/Object/" + sha_1.substring(0, 2) + "/" + sha_1.substring(2);
  }


  private static String shaHex(byte[] combined) {
   try{
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    byte[] hashbytes = md.digest(combined);
    return bytesToHex(hashbytes);
   }
   catch(NoSuchAlgorithmException e){
     throw new RuntimeException("Configured algorithm not found",e);
   }
  }

  private static String bytesToHex(byte[] hashbytes) {
    StringBuilder hexString = new StringBuilder();
    for(byte b :  hashbytes){
     String hex = Integer.toHexString(0xff & b);
     if(hex.length() == 1){
      //as each byte  is 2 hex digits, we need to pad with a 0
      hexString.append('0');
     }
  }
  return hexString.toString();
}

private static void ls_Tree(String[] args)  throws IOException{
  //get the tree object 
  //git ls-tree --name-only <tree-sha-1>
  if(args.length < 3){
    System.out.println("Usage: git ls-tree --name-only <tree-sha-1>");
    return;
  }

  boolean name_only = "--name-only".equals(args[1]);
  String tree_sha_1 = name_only ? args[2] : args[1];

  //get the tree object
  ArrayList<String> treeObjects = readTreeObjects(tree_sha_1);
  printTreeObjects(name_only,treeObjects);
}


private static  ArrayList<String> readTreeObjects(String tree_sha_1) throws IOException{
   ArrayList<String> treeEntries = new ArrayList<>();
   Path path =  Paths.get(get_path(tree_sha_1));
   
   try(InputStream is = Files.newInputStream(path)){
     //Decompress the data 
     InflaterInputStream iis = new InflaterInputStream(is);
     //as <20-byte-sha> is not hexadecimal format
     DataInputStream  dis = new DataInputStream(iis);

     // skip the header as first  20 bytes are the tree sha1
     dis.skipBytes(20);

     /*Each entry in a tree object has a mode. 
     a mode of 100644 [...] means it's a normal file. 
     Other options are 100755, which means it's an executable file;
     For directories, the value is 040000;
     */

     while(dis.available() > 0){
      StringBuilder  entry = new StringBuilder();

      //read mode
      //<mode> <name>
      StringBuilder mode = new StringBuilder();
      byte b;

      while((b = dis.readByte()) != ' '){
        mode.append((char)b);
      }
      entry.append(mode).append(' ');
      
      //read name
      // <name>\0<20_byte_sha>
      StringBuilder name = new StringBuilder();

      while((b = dis.readByte()) !=  '\0'){
       name.append((char)b);
   }
   entry.append(name);

   //read  sha1
   //<20_byte_sha>
   byte[]  sha1 = new byte[20];
   dis.readFully(sha1);
   entry.append(new String(sha1));

   treeEntries.add(entry.toString());

 }

} catch(IOException e){
  System.err.println("Error reading tree object: " + e.getMessage());
}

return treeEntries;

}

private static void printTreeObjects(boolean name_only, ArrayList<String> treeObjects){
  // print tree objects
  //sorted in alphabetical  order

  //list_Nme.sort(null); accepts  a comparator and if null is passed
  //defaults to natural order
  treeObjects.sort(null);
  if(name_only){
    treeObjects.stream()
               .map(objects -> objects.substring(' ') + 1)
               .forEach(System.out::println);
}
else{
   treeObjects.forEach(System.out::println);
}

}

private static void writeTreeObject() throws IOException{

  // Get Current Working Directory
  String curr_Direc = writeTree(Paths.get("."));
  System.out.println(curr_Direc);
}

private static String writeTree(Path curr_Direc) throws  IOException{
   ByteArrayOutputStream treeContent  = new ByteArrayOutputStream();
   Files.list(curr_Direc)
        .sorted().forEach(path -> writeRecursiveTreeObject(treeContent,curr_Direc,path));

    byte[] content = treeContent.toByteArray();
    String header =  "tree " + content.length + "\0";
    byte[] header_bytes =  header.getBytes(StandardCharsets.UTF_8);

    byte[] fullContent = new byte[content.length  + header_bytes.length];
    System.arraycopy(content,0,fullContent,0,content.length);
    System.arraycopy(header_bytes,0,fullContent,content.length,header_bytes.length);
    
    String treeHash = shaHex(fullContent);
    write_to_GitObject(treeHash, fullContent);

    return treeHash;
}

private static void writeRecursiveTreeObject(ByteArrayOutputStream treeContent, Path curr_Direc, Path path){

    try{
   String file_Type = curr_Direc.getFileName().toString();

   //check if it's a file  or a directory
   if(Files.exists(path)){
    if(Files.isRegularFile(path)){
      String file_Hash = computeHash(path.toString(),true);
      String mode = Files.isExecutable(path) ? "100755" :  "100644";
      WriteToTree(treeContent,mode,file_Type,file_Hash);
    }
    else if(Files.isDirectory(path)){
      String dir_Hash = writeTree(path);
       WriteToTree(treeContent,"40000",file_Type,dir_Hash);
    }else{
      System.err.println("Error : " + path.toString());
    }
   }
    
  }
  catch(IOException e){
    throw new UncheckedIOException(e);
  }

}


 private static void WriteToTree (ByteArrayOutputStream treeContent, String mode, String file_Type, String file_Hash) throws IOException{
  treeContent.write(String.format("%s %s\0", mode, file_Type).getBytes(StandardCharsets.UTF_8));
  treeContent.write(hexToBytes(file_Hash));
 }


 private static byte[] hexToBytes(String hash){
   byte[] bytes =  new byte[hash.length() / 2];
   for(int i = 0; i<=bytes.length-1; i++){
    bytes[i/2] =  (byte)((Character.digit(hash.charAt(i),16) << 4)
                           +  (Character.digit(hash.charAt(i+1),16)));

   } 
   return bytes;
 }

 private static void commitTree(String[] args) throws IOException{
    if(args.length < 6){
      System.out.println("Usage: java Main commit-tree <tree> -p  <parent> -m <message>");
    }

    String treeHash = args[1];
    String  parentHash = args[3];
    String message = args[5];
    String commitHash = commitObject(treeHash,parentHash,message);

    System.out.println("commit-Hash: " + commitHash);
 }

  private static String  commitObject(String treeHash, String parentHash, String message) throws IOException{
      String TimeStamp =  String.valueOf(System.currentTimeMillis());
      String committerName = "Arvind";
      String committerEmail = "arvind@gmail.com";

      StringBuilder  commitContent = new StringBuilder();
      
      commitContent.append("tree ").append(treeHash).append('\n')
                   .append("parent ").append(parentHash).append('\n')
                   .append("committerName ").append(committerName).append("committerEmial ").append(committerEmail).append(' ').append(TimeStamp).append('\n')
                   .append(message).append('\n');
              
     byte[]  commitContentBytes = commitContent.toString().getBytes(StandardCharsets.UTF_8);

     String commitHeader = "commit " + commitContentBytes.length + "\0";
     byte[]  commitHeaderBytes = commitHeader.getBytes(StandardCharsets.UTF_8);

     byte[] fullCommitContent = new byte[commitContentBytes.length + commitHeaderBytes.length];

     System.arraycopy(commitContentBytes,0,fullCommitContent,0,commitContentBytes.length);
     System.arraycopy(commitHeaderBytes,0,fullCommitContent,commitContentBytes.length,commitHeaderBytes.length);

     String  commitHash = bytesToHex(fullCommitContent);
     write_to_GitObject(commitHash, fullCommitContent);

     return commitHash;
  }

}