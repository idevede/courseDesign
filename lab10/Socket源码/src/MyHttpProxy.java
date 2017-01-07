

/**
1.�ȴ�����һ���ͻ��˵�����
2.����һ�����߳�����������
3.�����������ĵ�һ��(�����յ�url����)
4.���������һ�����յ�Http������������˿ں�
5.��һ�����յ�Http���������׽���
6.ͨ�����׽��ַ���/Http����
7.���ʹ��յ�Http������(ͨ���׽���)���ظ���������������
 * 
 */
import java.io.*;
import java.net.*;
public class MyHttpProxy extends Thread { 
	static public int CONNECT_RETRIES=5;	//������Ŀ���������Ӵ���
	static public int CONNECT_PAUSE=5;	//ÿ�ν������ӵļ��ʱ��
	static public int TIMEOUT=50;	//ÿ�γ������ӵ����ʱ��
	static public int BUFSIZ=1024;	//����������ֽ���
	static public boolean logging = false;	//�Ƿ��¼��־
	static public OutputStream log_S=null;	//��־�����
	static public OutputStream log_C=null;	//��־�����
	static public String LOGFILENAME_S="log_S.txt";
	static public String LOGFILENAME_C="log_C.txt";
	// ��ͻ���������Socket
	protected Socket csocket;	
    public MyHttpProxy(Socket cs) { 
	csocket=cs;
	start(); 
    }
    
    public void writeLog(int c, boolean browser) throws IOException {
    	if(browser) log_C.write((char)c);
    	else log_S.write((char)c);
    }

    public void writeLog(byte[] bytes,int offset, int len, boolean browser) throws IOException {
   	for (int i=0;i<len;i++) 
   		writeLog((int)bytes[offset+i],browser);
    }
    /**
     * ���̣߳���չthread�࣬��дrun������
		���ܣ�ʵ��һ���򵥵�״̬����ÿ�δ�web�ж�ȡһ���ַ�
		һֱ�������ռ����е���Ϣ���ҵ�Ŀ�������Ϊֹ��
		����ж�����������һ��
		�����ʹ����������������һ��������������׽���
		���׽��ֺ�run()���Ͳ������󲢵���pipe������
		����������׽���ssocket��csocket
		���������Ĵ��䡣
     */
    public void run(){
    	String buffer = "";		//��ȡ����ͷ
    	String URL="";			//��ȡ����URL
    	String host="";			//��ȡĿ������host
    	int port=80;			//Ĭ�϶˿�80
    	Socket ssocket = null;////��������������Ŀ����������׽��ֲ����Ի�
         //cisΪ�ͻ�����������sisΪĿ��������������socket�׽���
    	InputStream cis = null,sis=null;
         //cosΪ�ͻ����������sosΪĿ�����������
    	OutputStream cos = null,sos=null;	    	
       	try{
    		csocket.setSoTimeout(TIMEOUT);
    		cis=csocket.getInputStream();
    		cos=csocket.getOutputStream();
    		while(true){
    			int c=cis.read();//����http����������
    			if(c==-1) break;		//-1Ϊ��β��־
    			if(c=='\r'||c=='\n') break;//�����һ������
    			buffer=buffer+(char)c; //�õ�http������
    			if (logging) writeLog(c,true);
    		}
    	//��ȡURL(<a href="http://www.baidu.com/">http://www.baidu.com/</a>)  	
   		URL=getRequestURL(buffer);		
	
		int n;
    	//��ȡhost
  		n=URL.indexOf("//");
 		if (n!=-1) 	
                		host=URL.substring(n+2);	// www.baidu.com/
  		n=host.indexOf('/');
   		if (n!=-1) 	
                  		host=host.substring(0,n);// www.baidu.com
    	    
    	// �������ܴ��ڵĶ˿ں�
  		n=host.indexOf(':');
   		if (n!=-1) { 
   			port=Integer.parseInt(host.substring(n+1));
   			host=host.substring(0,n);
  		}
   		int retry=CONNECT_RETRIES;
   		while (retry--!=0) {
   			try {
   				//��Ŀ���������������
    				ssocket=new Socket(host,port);	//���Խ�����Ŀ������������
    				break;
    			} catch (Exception e) { }
                 		// �ȴ�
   			Thread.sleep(CONNECT_PAUSE);//�̵߳ȴ�������
   		}
   		if(ssocket!=null){
   			ssocket.setSoTimeout(TIMEOUT);//�����׽��ֳ�ʱʱ��
   			sis=ssocket.getInputStream();
   			sos=ssocket.getOutputStream();//��sscoket(��outbound)�еõ������
   			sos.write(buffer.getBytes());		//������ͷд�룬��Ŀ����������http����
   			pipe(cis,sis,sos,cos);				//����ͨ�Źܵ�
   		}    			
          	}catch(Exception e){
    		e.printStackTrace();
    	}
    	finally {
		try { 
		    	csocket.close();
		    	cis.close();
		    	cos.close();
				
		} 
		catch (Exception e1) {
		    	System.out.println("\nClient Socket Closed Exception:");
		    	e1.printStackTrace();
		}
		try { 
		    	ssocket.close();
		    	sis.close();
		    	sos.close();
		} 
		catch (Exception e2) {
		    	System.out.println("\nServer Socket Closed Exception:");
		    	e2.printStackTrace();
		}
       	}
    }
    public String getRequestURL(String buffer){
    	String[] tokens=buffer.split(" ");
    	String URL="";
    	for(int index=0;index<tokens.length;index++){
    		if(tokens[index].startsWith("http://")){
    			URL=tokens[index];
    			break;
    		}
    	}
    	return URL;    	
    }
    /**
     * ����������׽���֮���������Ĵ���
     */
    public void pipe(InputStream cis,InputStream sis,OutputStream sos,OutputStream cos){
    	try {
    	    int length;
    	    byte bytes[]=new byte[BUFSIZ];//�����ֽ�����
    	    while (true) {
    	    	try {
    	    		//��cis�еõ���ͨ��sos��������
    	    		if ((length=cis.read(bytes))>0) {
    	    			sos.write(bytes,0,length);
    	    			if (logging) writeLog(bytes,0,length,true); 	    			
    	    		}
    	    		else if (length<0)
    	    			break;
    	    	}
    	    	catch(SocketTimeoutException e){}
    	    	catch (InterruptedIOException e) { 
    	    		System.out.println("\nRequest Exception:");
    	    		e.printStackTrace();
    	    	}
    	    	try {
    	    		if ((length=sis.read(bytes))>0) {
    	    			cos.write(bytes,0,length);
    	    			if (logging) writeLog(bytes,0,length,false);
    	    		}
    	    		else if (length<0) 
    	    			break;
    	    	}
    	    	catch(SocketTimeoutException e){}
    	    	catch (InterruptedIOException e) {
    	    		System.out.println("\nResponse Exception:");
    		    	e.printStackTrace();
    	    	}
    	    }
    	} catch (Exception e0) {
    	    System.out.println("Pipe�쳣: " + e0);
    	}		
    }
/**
 * 
 * @param port �˿ںţ�����server�׽��֣����ڼ���web�ͻ�������
 * @param clobj �����
 * ͨ�������clobj�ҵ���Ӧ�Ĺ��캯��
 */
public static  void startProxy(int port,Class clobj) { 
    try { 
        ServerSocket ssock=new ServerSocket(port); //�����������port�˿ڼ���web�ͻ�������
        while (true) { 
        Class [] sarg = new Class[1]; //����������
        Object [] arg= new Object[1]; //������������
        sarg[0]=Socket.class; 	//�ำֵ
        try { 
        java.lang.reflect.Constructor cons = clobj.getDeclaredConstructor(sarg); //���ҹ��캯��
        arg[0]=ssock.accept(); 
        cons.newInstance(arg); // ����HttpProxy�����������ʵ�� 
        } catch (Exception e) { 
        Socket esock = (Socket)arg[0]; 
        try { esock.close(); } catch (Exception ec) {} 
        } 
        } 
    } catch (IOException e) { 
    System.out.println("\nStartProxy Exception:"); 
    e.printStackTrace(); 
    } 
    } 


        // �����õļ�main���� 
    static public void main(String args[]) throws FileNotFoundException { 
    System.out.println("�ڶ˿�808�������������\n"); 
    OutputStream file_S=new FileOutputStream(new File(LOGFILENAME_S)); 
    OutputStream file_C=new FileOutputStream(new File(LOGFILENAME_C)); 
    MyHttpProxy.log_S=file_S; 
    MyHttpProxy.log_C=file_C; 
    MyHttpProxy.logging=true; 
    MyHttpProxy.startProxy(808,MyHttpProxy.class); 
    }

 
 }
