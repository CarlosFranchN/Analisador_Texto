package com.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.jocl.CL;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueueWithProperties;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;


public class App 
{

    private static final String KERNEL_CODE =
    "__kernel void count_word(" +
    "    __global const char *text," +
    "    __global const char *word," +
    "    const int text_length," +
    "    const int word_length," +
    "    __global int *count) {" +
    "    const char case_diff = 'a' - 'A';" +
    "    int id = get_global_id(0);" +
    "    if (id + word_length > text_length) {" +
    "        return;" +
    "    }" +
    "    int match = 1;" +
    "    for (int i = 0; i < word_length; i++) {" +
    "        char text_char = text[id + i];" +
    "        char word_char = word[i];" +
    "        if (text_char >= 'A' && text_char <= 'Z') {" +
    "            text_char += case_diff;" +
    "        }" +
    "        if (word_char >= 'A' && word_char <= 'Z') {" +
    "            word_char += case_diff;" +
    "        }" +
    "        if (text_char != word_char) {" +
    "            match = 0;" +
    "            break;" +
    "        }" +
    "    }" +
    "    if (match) {" +
    "        int is_start_valid = (id == 0 || text[id - 1] == ' ' || text[id - 1] == '\\n' || text[id - 1] == '.' || text[id - 1] == ',');" +
    "        int is_end_valid = (id + word_length == text_length || text[id + word_length] == ' ' || text[id + word_length] == '\\n' || text[id + word_length] == '.' || text[id + word_length] == ',');" +
    "        if (is_start_valid && is_end_valid) {" +
    "            atomic_add(count, 1);" +
    "        }" +
    "    }" +
    "}";
    public static void main( String[] args )
    {

        // String texto = readFile("test/src/main/java/com/files/carlosText.txt");
        String texto = readFile("test\\src\\main\\java\\com\\files\\DonQuixote-388208.txt");
        // String texto = readFile("test\\src\\main\\java\\com\\files\\Dracula-165307.txt");
        // String texto = readFile("test\\src\\main\\java\\com\\files\\MobyDick-217452.txt");

        



        try {
            gerarCsv("DonQuixote" , texto, "Quijote");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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

    static  int contadorPalavra(String texto, String keyword){
        String[] arr = stringToArray(texto);
        int counter = 0;
        if (arr.length > 0){
            for (String word : arr) {
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

    static byte[] transformarBytes(String texto){
        String[] arr = stringToArray(texto);
        StringBuilder textoConcatenado = new StringBuilder();
        for (String p : arr) {

            textoConcatenado.append(p).append(" ");
        }
        byte[] textoBytes = textoConcatenado.toString().trim().getBytes(StandardCharsets.UTF_8);
        return  textoBytes;
    }

    static  void startingOpenCL(String texto, String palavra){
        byte[] textoBytes = transformarBytes(texto);
        byte[] palavraBytes = palavra.getBytes(StandardCharsets.UTF_8);


       // OpenCL initialization
       CL.setExceptionsEnabled(true);
       int platformIndex = 0;
       int deviceIndex = 0;
       long deviceType = CL.CL_DEVICE_TYPE_GPU;
       cl_platform_id[] platforms = new cl_platform_id[1];
       clGetPlatformIDs(1, platforms, null);
       cl_platform_id platform = platforms[platformIndex];
       cl_device_id[] devices = new cl_device_id[1];
       clGetDeviceIDs(platform, deviceType, 1, devices, null);
       cl_device_id device = devices[deviceIndex];
       cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
       cl_command_queue queue = clCreateCommandQueueWithProperties(context, device, null, null);

       // Create buffers
       cl_mem textBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, textoBytes.length, Pointer.to(textoBytes), null);
       cl_mem wordBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, palavraBytes.length, Pointer.to(palavraBytes), null);
       cl_mem countBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_int, Pointer.to(new int[]{0}), null);

       // Create and build program
       cl_program program = clCreateProgramWithSource(context, 1, new String[]{KERNEL_CODE}, null, null);
       clBuildProgram(program, 0, null, null, null, null);

       // Create kernel
       cl_kernel kernel = clCreateKernel(program, "count_word", null);

       // Set kernel arguments
       clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(textBuffer));
       clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(wordBuffer));
       clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{textoBytes.length}));
       clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{palavraBytes.length}));
       clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(countBuffer));

       // Execute kernel
       long[] globalWorkSize = new long[]{textoBytes.length};
       clEnqueueNDRangeKernel(queue, kernel, 1, null, globalWorkSize, null, 0, null, null);

       // Read result
       int[] count = new int[1];
       clEnqueueReadBuffer(queue, countBuffer, CL.CL_TRUE, 0, Sizeof.cl_int, Pointer.to(count), 0, null, null);

       // Release resources
       clReleaseKernel(kernel);
       clReleaseProgram(program);
       clReleaseMemObject(textBuffer);
       clReleaseMemObject(wordBuffer);
       clReleaseMemObject(countBuffer);
       clReleaseCommandQueue(queue);
       clReleaseContext(context);

       // Output result
       System.out.println("A palavra \"" + palavra + "\" aparece " + count[0] + " vezes no texto.");

    }


    static long  gerarTestesSerial(String texto, String keyword){
        
        long inicio = System.currentTimeMillis();
        int qtd = contadorPalavra(texto, keyword);
        long fim = System.currentTimeMillis();

        System.out.println("Serial");
        System.out.println("A palavra \"" + keyword + "\" aparece " + qtd + " vezes no texto.");
        System.out.println((fim-inicio)  + " Milisegundos");
        return  fim-inicio;
    }
    static long gerarTestesGPU(String texto, String keyword){
        long inicio_gpu = System.currentTimeMillis();
        System.out.println("GPU");
        startingOpenCL(texto, keyword);
        long fim_gpu = System.currentTimeMillis();
        System.out.println(fim_gpu -inicio_gpu  + " Milisegundos");
        return fim_gpu -inicio_gpu;
    }

    static String gerarCsv(String titulo, String texto, String keyword) throws IOException {
                
        // String  newTitulo = titulo + "_" + "x" + "_" + keyword + "(" + qtd + ")";  
        String  newTitulo = titulo + "_" + "x" + "_" + keyword;  
        String path = "test/src/main/java/com/csv/" + newTitulo + ".csv";
        
        
        FileWriter writer = null;
        
        try {
            
            File file = new File(path);
            String originalPath = path;
            int counter = 1;
    
            
            while (file.exists()) {
                String newPath = originalPath.replace(".csv", "_" + counter + ".csv");
                file = new File(newPath);
                path = newPath;  
                counter++;
            }
    
            writer = new FileWriter(path);  
           
            writer.append("Amostra,palavra-chave,quantidade,serial,gpu\n");

            int num_amostra = 1;
            while (num_amostra < 4) {
                int numero = contadorPalavra(texto, keyword);
                long tempoSerial = gerarTestesSerial(texto, keyword);
                long tempoGpu = gerarTestesGPU(texto, keyword);
                writer.append(num_amostra + "," );
                writer.append(keyword + ",");
                writer.append(numero + ",");
                writer.append( tempoSerial + ",");
                writer.append( tempoGpu + "\n");
                num_amostra++;
            }


            System.out.println("Arquivo CSV gerado com sucesso! Caminho: " + path);
    
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;
        }


}
