package com.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import sun.security.util.Length;


public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        String texto = readFile("test/src/main/java/com/files/carlosText.txt");
        // System.out.println(texto);
        int palavras = texto.split('\\s').length;
        
    }

    static  String readFile(String arquivo){
        String finalData = "";
        try {
            File myArq = new File(arquivo);
            Scanner myReader = new Scanner(myArq);
            while(myReader.hasNextLine()){
                String data = myReader.nextLine();
                // System.out.println(data);
                finalData += data;
            }
            // System.out.println(finalData);
            myReader.close();            
            return finalData;
        } catch (FileNotFoundException e) {
            System.out.println("Erro boy");
            e.printStackTrace();
        }
        return  finalData;
        


    }
}
