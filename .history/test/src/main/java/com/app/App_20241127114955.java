package com.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        readFile("test/src/main/java/com/files/carlosText.txt");
    }

    static  void readFile(String arquivo){
        try {
            File myArq = new File(arquivo);
            Scanner myReader = new Scanner(myArq);
            String finalData = "";
            while(myReader.hasNextLine()){
                String data = myReader.nextLine();
                // System.out.println(data);
                finalData += data;
            }
            System.out.println(finalData);
            myReader.close();            
        } catch (FileNotFoundException e) {
            System.out.println("Erro boy");
            e.printStackTrace();
        }
        


    }
}
