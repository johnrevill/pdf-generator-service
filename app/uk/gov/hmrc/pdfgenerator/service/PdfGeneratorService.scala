package uk.gov.hmrc.pdfgenerator.service

import java.io.{BufferedWriter, File, FileWriter}

import uk.gov.hmrc.play.http.BadRequestException

import scala.concurrent.Future


object PdfGeneratorService extends PdfGeneratorService


/**
  * Created by habeeb on 10/10/2016.
  *
  * This class is responsible for generating a PDF from a given
  * HTML page
  *
  */
trait PdfGeneratorService {

//  private val GS_ALIAS = "gs"
  private val GS_ALIAS = "/app/bin/gs-920-linux_x86_64"
//  private val baseDir: String = "/Users/work/Desktop/temp/"
//  private val baseDir: String = new File(".").getCanonicalPath + "/"
  private val baseDir: String = "/app/" // will eventually become "/app/scratch/"
//  private val CONF_DIR = "/Users/work/Desktop/temp/"
  private val CONF_DIR = "/app/"

  private val pdfs_def = "PDFA_def.ps"
  private val PDFA_CONF = CONF_DIR + pdfs_def
  private val adobeColorProfile = "AdobeRGB1998.icc"
  private val ICC_CONF = CONF_DIR + adobeColorProfile

  def generatePdfFromHtml(html : String, outputFileName : String) : File = {
    import java.io._

    import io.github.cloudify.scala.spdf._

    if(html == null || html.isEmpty){
      Future.failed(throw new BadRequestException("Html must be provided"))
    }

    if(outputFileName == null || outputFileName.isEmpty){
      Future.failed(throw new BadRequestException("OutputFileName must be provided"))
    }

          val pdf = Pdf("/app/bin/wkhtmltopdf", new PdfConfig {
            orientation := Portrait
            pageSize := "A4"
            marginTop := "1in"
            marginBottom := "1in"
            marginLeft := "1in"
            marginRight := "1in"
            disableExternalLinks := true
            disableInternalLinks := true
          })


//    // Create a new Pdf converter with a custom configuration
//    // run `wkhtmltopdf --extended-help` for a full list of options
//    val pdf = Pdf(new PdfConfig {
//      orientation := Portrait
//      pageSize := "A4"
//      marginTop := "1in"
//      marginBottom := "1in"
//      marginLeft := "1in"
//      marginRight := "1in"
//    })

    val destinationDocument: File = new File(outputFileName)
    pdf.run(html, destinationDocument)

    return destinationDocument
  }

  def convertToPdfA(inputFileName : String, outputFileName : String) : File = {
    import scala.sys.process.Process

    setUpConfigFile(pdfs_def, PDFA_CONF)
    setUpConfigFile(adobeColorProfile, ICC_CONF)

    val command: String = GS_ALIAS + " -dPDFA=1 -dPDFACompatibilityPolicy=1  -dNOOUTERSAVE -sProcessColorModel=DeviceRGB " +
      "-sDEVICE=pdfwrite -o " + outputFileName + " " + PDFA_CONF + "  " + inputFileName
    val pb = Process(command)
    val exitCode = pb.!

    return new File(outputFileName)
  }

  def generateCompliantPdfA(html : String, inputFileName : String, outputFileName : String) : File = {
    import scala.sys.process.Process

    //code will be refactored

    val file: File = generatePdfFromHtml(html, baseDir + inputFileName)

    val pdfA: File = convertToPdfA(baseDir + inputFileName, baseDir + outputFileName)

    val deleteCommand: String = "rm -Rf" + " " + baseDir + inputFileName
    val pd = Process(deleteCommand)
    val exitCodeTwo = pd.!

    return pdfA
  }

  def setUpConfigFile(fileName:String, configPath:String) : Unit = {

    if(new File(configPath).exists){
      return
    }

    val bytes = ResourceHelper.reader("/"+fileName)
    ResourceHelper.writer(configPath, bytes)

//    val file = new File(configPath)
//    val bw = new BufferedWriter(new FileWriter(file))
//
//    val inputStream = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/" + fileName))
//
//    bw.write(inputStream.mkString)
//    bw.close()
  }

//  def toSource(fileName:String, encoding:String): scala.io.BufferedSource = {
//    import java.nio.charset.{Charset, CodingErrorAction}
//
//    val resource = getClass.getResourceAsStream("/" + fileName)
//
//    val decoder = Charset.forName(encoding).newDecoder()
//    decoder.onMalformedInput(CodingErrorAction.IGNORE)
//    scala.io.Source.fromInputStream(resource)(decoder)
//  }

}