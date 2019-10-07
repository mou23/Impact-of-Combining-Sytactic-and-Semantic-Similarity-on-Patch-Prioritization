package dataprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

public class DataProcessor {
	public static void main(String[] args) {
		int total = 0;
		File sourceFolder = new File("D:/thesis/all/");
		File destinationFaultLocation = new File("dataset/fault location/");
		File destinationBuggy = new File("dataset/buggy/");
		File destinationFixed = new File("dataset/fixed/");
		File destinationProject = new File("dataset/project/");
		File[] listOfFolders = sourceFolder.listFiles();
		int counter = 0;
		//		System.out.println(listOfFiles.length);
		try {
			for (int i = 0; i < listOfFolders.length; i++) {
				//				System.out.println(listOfFolders[i]);
				File currentFolder = new File(listOfFolders[i]+"/modifiedFiles");
				if(currentFolder.exists()) {
//					String project = ""; 
//					ArrayList<String> commits = new ArrayList<>();
					
//					File[] allFile = listOfFolders[i].listFiles();
//					for(int j = 0; j<allFile.length; j++) {
//						if(allFile[j].getName().endsWith("projecturl")) {
//							BufferedReader fileReader = new BufferedReader(new FileReader(allFile[j]));
//							
//							String line;
//							while ((line = fileReader.readLine()) != null) {
//								project = line;
//							}
//							fileReader.close();
//						}
//						if(allFile[j].getName().endsWith("withUnitTest.txt")) {
//							BufferedReader fileReader = new BufferedReader(new FileReader(allFile[j]));
//							commits.add("");
//							String line;
//							while ((line = fileReader.readLine()) != null) {
//								String[] str = line.split(" ");
//								commits.add(str[0]);
////								System.out.println(str[0]);
//							}
//							fileReader.close();
//						}
//					}
//					if(commits.size()==0) {
//						System.out.println(listOfFolders[i] + "problem!!!!");
//						continue;
//					}
					ArrayList<String> filenames = new ArrayList<String>();
					File[] listOfFilesInCurrentFolder = currentFolder.listFiles();
					for (int j = 0; j < listOfFilesInCurrentFolder.length; j++) {
						counter++;
						File faultyFileFolder = new File(listOfFilesInCurrentFolder[j]+"/old");
						File faultyFile = faultyFileFolder.listFiles()[0];
						filenames.add(faultyFile.getName());
						File fixedFileFolder = new File(listOfFilesInCurrentFolder[j]+"/fix");
						File fixedFile = fixedFileFolder.listFiles()[0];

						BufferedReader faultyFileReader = new BufferedReader(new FileReader(faultyFile));
						ArrayList<String> faultyFileLines = new ArrayList<>();
						String line;
						while ((line = faultyFileReader.readLine()) != null) {
							faultyFileLines.add(line);
						}
						faultyFileReader.close();


						BufferedReader fixedFileReader = new BufferedReader(new FileReader(fixedFile));
						ArrayList<String> fixedFileLines = new ArrayList<>();
						while ((line = fixedFileReader.readLine()) != null) {
							fixedFileLines.add(line);
						}
						fixedFileReader.close();

						if(fixedFileLines.size()!=faultyFileLines.size()) {
							continue;
						}

						int mismatch = 0;
						int index = -1;
						for(int k = 0; k<fixedFileLines.size(); k++) {
							if(!fixedFileLines.get(k).equals(faultyFileLines.get(k))) {
								mismatch++;
								index = k+1;
							}
						}
//						System.out.println(commits);
						if(mismatch==1) {
							for(int k=0; k<filenames.size()-1; k++) {
								if(filenames.get(k).equals(faultyFile.getName())) {
									File tempFault = new File(listOfFilesInCurrentFolder[k]+"/old").listFiles()[0];
//									System.out.println("Check: " +faultyFile.getAbsolutePath() + ","+tempFault.getAbsolutePath());
									BufferedReader tempFileReader = new BufferedReader(new FileReader(tempFault));
									ArrayList<String> tempFileLines = new ArrayList<>();
									while ((line = tempFileReader.readLine()) != null) {
										tempFileLines.add(line);
									}
									tempFileReader.close();
									
									if(tempFileLines.size()!=faultyFileLines.size()) {
										continue;
									}
									boolean duplicate = true;
									for(int l = 0; l<tempFileLines.size(); l++) {
										if(!tempFileLines.get(l).equals(faultyFileLines.get(l))) {
											duplicate = false;
											break;
										}
									}
									
									if(duplicate==false) {
										continue;
									}
									
									File tempFix = new File(listOfFilesInCurrentFolder[k]+"/fix").listFiles()[0];
									tempFileReader = new BufferedReader(new FileReader(tempFix));
									tempFileLines.clear();
									while ((line = tempFileReader.readLine()) != null) {
										tempFileLines.add(line);
									}
									tempFileReader.close();
									
									if(tempFileLines.size()!=fixedFileLines.size()) {
										continue;
									}
									for(int l = 0; l<tempFileLines.size(); l++) {
										if(!tempFileLines.get(l).equals(fixedFileLines.get(l))) {
											duplicate = false;
											break;
										}
									}
									
									if(duplicate == true) {
										System.out.println(faultyFile.getParentFile().getParent());
										break;
									}
								}
							}
							
//							System.out.println(listOfFilesInCurrentFolder[j]+ ","+project+"/archive/"+commits.get(Integer.parseInt(listOfFilesInCurrentFolder[j].getName())-1));
//							String file = listOfFilesInCurrentFolder[j]+"/"+faultyFile.getName();
//							file = file.substring(file.indexOf(sourceFolder.getName())+sourceFolder.getName().length());
//							System.out.println(destinationFaultLocation+file);
//							File f = (new File(destinationFaultLocation+file));
//							f.getParentFile().mkdirs();
//							try{    
//								FileWriter  fw=new FileWriter(destinationFaultLocation+file);    
//								fw.write(index+"\n");    
//								fw.close();    
//							} catch(Exception e){
//								System.out.println(e.getMessage());
//							}  						
//							f = (new File(destinationBuggy+file));
//							f.getParentFile().mkdirs();
//							
//							FileUtils.copyFile(faultyFile, f);
//							
//							f = (new File(destinationFixed+file));
//							f.getParentFile().mkdirs();
//							
//							FileUtils.copyFile(fixedFile, f);
//							
//							System.out.println("file " + listOfFilesInCurrentFolder[j]);
//							System.out.println("faulty line "+faultyFileLines.get(index-1));
//							System.out.println("fixing line "+fixedFileLines.get(index-1));
//							System.out.println();
							total++;
						}
					}

				}
				else {
//					System.out.println(currentFolder.getName() + "doesn't exist");
				}
//				break;
			}
//			System.out.println("total one-line patch "+total);
//			System.out.println("total patches "+counter);
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
