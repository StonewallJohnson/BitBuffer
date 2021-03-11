import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;

public class BitBuffer{
    File file;
    boolean bits[];
    int index;
    int buffSize;
    boolean writable;
    BufferedOutputStream outBuff;
    BufferedInputStream inBuff; 

    /**
     * Initializes a BitBuffer operating on a given
     * file with the given mode
     * @param f the file to operate on
     * @param mode the mode in which the file is handled: 1 -> write, 0 -> read
     */
    public BitBuffer(File f, boolean mode){
        file = f;
        buffSize = 128; //16 bytes
        bits = new boolean[buffSize];
        index = 0;
        writable = mode;
        
        try{
            if(writable){
                //mode is writing
                FileOutputStream fos = new FileOutputStream(file);
                outBuff = new BufferedOutputStream(fos);
            }
            else{
                //mode is reading
                //TODO: populate the buffer with first bytes
                FileInputStream fis = new FileInputStream(file);
                inBuff = new BufferedInputStream(fis);
            }
        }
        catch(IOException q){
            q.printStackTrace();
        }
    }

    /**
     * Writes one bit to the bit buffer
     * @param bitVal the value of the bit to be buffered
     */
    public void writeBit(boolean bitVal){
        if(writable){
            //can write to buffer
            if(index >= buffSize){
                //the buffer is full
                writeBuff();
            }
            //buffer not full, insert new bit to buffer
            bits[index] = bitVal;
            index++;
        }
        else{
            System.out.println("Can not perform operation, buffer is not in write mode.");
        }
    }

    /**
     * Writes the contents of the buffer to the file
     * and cleans the buffer
     */
    public void writeBuff(){
        for(int i = 0; i < index; i += 8){
            //for the minimum number of bytes to write from buffer
            Byte b = 0x0;
            for(int j = i; j < i + 8; j++){
                //for every bit
                b = (byte)(b << 1); 
                if(!bits[j]){
                    //flip bottom bit to 0
                    b =  (byte)(b ^ 0x1);
                }
            }
            
            //write assembled byte to file buffer
            try{
                outBuff.write(b);
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }

        for(int i = 0; i < buffSize; i++){
            //for every bit in the buffer
            bits[i] = false;
        }
        index = 0;
    }

    /**
     * Empties the buffer and closes the file
     */
    public void close(){
        writeBuff();
        try{
            outBuff.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
    
}