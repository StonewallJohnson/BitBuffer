import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;

public class BitBuffer{
    private File file;
    private boolean bits[];
    private boolean moreBits[];
    private final boolean eofBits[] = new boolean[]{false, false, false, false, false, false, true, true};
    private boolean extraInUse;
    private int index;
    private final int buffSize = 128;
    private boolean writable;
    private BufferedOutputStream outBuff;
    private BufferedInputStream inBuff; 

    /**
     * Initializes a BitBuffer operating on a given
     * file with the given mode
     * @param f the file to operate on
     * @param mode the mode in which the file is handled: 1 -> write, 0 -> read
     */
    public BitBuffer(File f, boolean mode){
        file = f;
        bits = new boolean[buffSize];
        index = 0;
        writable = mode;
        extraInUse = false;
        
        try{
            if(writable){
                //mode is writing
                FileOutputStream fos = new FileOutputStream(file);
                outBuff = new BufferedOutputStream(fos);
            }
            else{
                //mode is reading
                FileInputStream fis = new FileInputStream(file);
                inBuff = new BufferedInputStream(fis);
                fillBuff(bits);
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
            System.out.println("Can not perform operation; mode is read.");
        }
    }

    /**
     * Writes the important bits of a byte to the buffer
     * @param byt the byte to be compressed and written
     */
    public void compressByte(byte byt){
        if(writable){
            int i = 0;
            byte mask = (byte) ( 0x1 << (7 - i) );
            
            while( ((byt & mask) == 0) && (i < 8)){
                //until a set bit is found
                i++;
                mask = (byte) ( 0x1 << (7 - i) );
            }
    
            while(i < 8){
                //all remaining bits
                mask = (byte) ( 0x1 << (7 - i) );
                writeBit((byt & mask) != 0);
                i++;
            }
        }
        else{
            System.out.println("Can not perform operation; mode is read.");
        }
    }
    
    /**
     * Writes a whole byte to the bit buffer
     * @param byt the byte to be written
     */
    public void writeByte(byte byt){
        if(writable){
            int i = 0;
            byte mask;
            while(i < 8){
                //for every bit
                mask = (byte) ( 0x1 << (7 - i) );
                writeBit((byt & mask) != 0);

                i++;
            }
        }
        else{
            System.out.println("Can not perform operation; mode is read.");
        }
    }

    /**
     * Writes the important bits of a character to the buffer
     * @param c the character to be compressed
     */
    public void compressChar(char c){
        compressByte((byte) c);
    }

    /**
     * Writes the contents of the buffer to the file
     * and cleans the buffer
     */
    private void writeBuff(){
        for(int i = 0; i < index; i += 8){
            //for the minimum number of bytes to write from buffer
            Byte b = 0x0;
            for(int j = i; j < i + 8; j++){
                //for every bit
                b = (byte)(b << 1); 
                if(bits[j]){
                    //flip bottom bit to 1
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

        for(int i = 0; i < index; i++){
            //for every bit in the buffer
            bits[i] = false;
        }
        index = 0;
    }

    /**
     * Empties the buffer and closes the file
     */
    public void close(){
        if(writable){
            writeBuff();
            try{
                outBuff.close();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
        else{
            try{
                inBuff.close();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * @return the next bit value from the buffer
     */
    public boolean readBit(){
        boolean bitVal;
        if(!writable){
            //can read this buffer
            if(index >= buffSize){
                //get more to read
                fillBuff(bits);
            }
            bitVal = bits[index];
            index++;
        }
        else{
            bitVal = false;
            System.out.println("Can not do this operation; mode is write.");
        }
        return bitVal;
    }

    /**
     * Reads a whole byte from the bit buffer
     * @return the byte taken from the buffer
     */
    public byte readByte(){
        byte b = 0;
        if(!writable){
            //can read
            int i = 0;
            while(i < 8){
                //for every bit in the byte
                boolean bit = readBit();
                b = (byte) (b << 1);
                if(bit){
                    //set a 1 in the bottom bit
                    b = (byte) (b | 0x1);
                }
                i++;
            }
        }
        else{
            System.out.println("Can not perform operation; mode is write.");
        }

        return b;
    }
    
    /**
     * Fills the buffer with the next inputs from the file
     */
    private void fillBuff(boolean buf[]){
        index = 0;
        if(buf == bits && extraInUse){
            //we already have the bits to use
            bits = moreBits;
            extraInUse = false;
        }
        else{
            for(int i = 0; i < buffSize; i += 8){
                //for every byte in the buffer
                Byte b;
                try{
                    b = (byte) inBuff.read();
                    for(int j = i; j < i + 8; j++){
                        //for every bit in the byte
                        byte mask = (byte) ( 0x1 << (8 - (j % 8) - 1) );
                        if((b & mask) != 0){
                            //bit is set
                            buf[j] = true;
                        }
                        else{
                            buf[j] = false;
                        }
                    }
                } 
                catch(IOException e){
                    e.printStackTrace();
                }
            }
        }

        index = 0;
    }

    /**
     * 
     * @return true if the next two bytes of the buffer are
     * 0, false if not
     */
    public boolean hasEOF(){
        boolean oldMode = writable;
        writable = false;
        int oldIndex = index;
        boolean EOF;
        
        if(index + 8 >= buffSize){
            //we have a buffer problem
            //grab the next buffSize bits because that is
            //how fillBuff works
            boolean specialBits[] = new boolean[8];
            int i = 0;
            while(index < buffSize){
                //check for end of file
                specialBits[i] = readBit();
                i++;
            }
            fillBuff(moreBits);
            extraInUse = true;
            int j = 0;
            while(i < 8){
                //there are more bits to scan from the new bits
                specialBits[i] = moreBits[j];
                j++;
            }

            for(int q = 0; i < 8; i++){
                //every bit in specialBits
                if(specialBits[q] != eofBits[q]){
                    writable = oldMode;
                    index = oldIndex;    
                    return false;
                }
            }
            
            EOF = true;
        }
        else{
            //no buffer problem
            if(readByte() == 3){
                //ETX character found
                EOF = true;
            }
            else{
                EOF = false;
            }
        }

        writable = oldMode;
        index = oldIndex;
        return EOF;
    }
}