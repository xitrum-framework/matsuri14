package matsuri.demo.util

import java.util.Date
import java.text.{ParseException, SimpleDateFormat}

object Converter {

  val parser_yyyymmddhhmm = new SimpleDateFormat("yyyyMMddHHmm");
      parser_yyyymmddhhmm.setLenient(false)
  val format_yyyymmddhhmm_with_slash = new SimpleDateFormat("yyyy/MM/dd HH:mm");

  def yyyyMMddHHmm2UnixTime(yyyyMMddHHmm:String):Long = {
    try {
      val d = parser_yyyymmddhhmm.parse(yyyyMMddHHmm)
      d.getTime / 1000
    } catch {
      case e:Exception => 0
    }
  }

  def unixTime2yyyyMMddHHmm(unixtime: Long):String = {
    try {
      val d = new Date(unixtime * 1000)
      format_yyyymmddhhmm_with_slash.format(d)
    } catch {
      case e:Exception => "invalid date"
    }
  }

  def ts2Date(ts:Int):Date = {
    new Date(ts.toLong * 1000)
  }

  def tryAsLong(x:Any):Long = {
    try{
      x.asInstanceOf[Int].toLong
    } catch {
      case e:Exception => 0.toLong
   }
  }
}