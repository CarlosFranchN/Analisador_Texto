package com.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        String texto = readFile("test/src/main/java/com/files/carlosText.txt");
        String[] arr = stringToArray(texto);
        for (String word : arr) {
            System.out.println(word);
            
        }
        int qtd = contadorPalavra(arr, "carlos");
        System.out.println(qtd);
        // System.out.println(texto);
        
        
    }

    static  String readFile(String arquivo){
        StringBuilder conteudo = new StringBuilder();
        try {
            File myArq = new File(arquivo);
            Scanner myReader = new Scanner(myArq);
            while(myReader.hasNextLine()){
                String data = myReader.nextLine();
                // System.out.println(data);
                conteudo.append((data)).append("\n");
            }
            // System.out.println(finalData);
            myReader.close();            
            // return conteudo.toString();
        } catch (FileNotFoundException e) {
            System.out.println("Erro boy");
            e.printStackTrace();
        }
        return  conteudo.toString();
    }
    static  String[] stringToArray(String texto){
        String[] palavras = texto.split("[\\s,\\.\\n\\r]+");
        return  palavras;
    }

    static  int contadorPalavra(String [] texto, String keyword){
        int counter = 0;
        if (texto.length > 0){
            for (String word : texto) {
                if(word.equalsIgnoreCase(keyword)){
                    counter++;
                }
            }
        }else{
            System.out.println("Texto Vazio");
        }
        if(counter == 0){
            System.out.println("A palavra não foi encontrada!");
        }
        return counter;

    }
}
