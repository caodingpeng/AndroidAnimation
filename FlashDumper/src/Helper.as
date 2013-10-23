package
{
	
	import com.juicebeer.component.Console;
	
	import fl.text.TLFTextField;
	
	import flash.desktop.NativeProcess;
	import flash.desktop.NativeProcessStartupInfo;
	import flash.display.BitmapData;
	import flash.display.DisplayObject;
	import flash.display.DisplayObjectContainer;
	import flash.events.IOErrorEvent;
	import flash.events.NativeProcessExitEvent;
	import flash.events.ProgressEvent;
import flash.filesystem.File;
	import flash.filesystem.FileMode;
	import flash.filesystem.FileStream;
	import flash.geom.Matrix;
	import flash.geom.Rectangle;
	import flash.system.Capabilities;
	import flash.text.TextField;
	import flash.text.engine.TextLine;
	import flash.utils.ByteArray;
	
	import mx.graphics.codec.PNGEncoder;

	import spark.components.TextArea;

	public class Helper
	{
		public var swf:DisplayObjectContainer;
		public var logArea:TextArea;

		public static var basePath:String="export/";
        public static var swfName:String="";
        public static var sheetConfig:Object=loadJsonFile('sheet_config.json');
		public var currentBasePath:String;

        public static var findShareFile:Boolean=false;
		
		public var errorDisplayObjectSnapshot:BitmapData;
		
		private static var _instance:Helper;
		public static function instance():Helper
		{
			if(_instance==null)
			{
				_instance = new Helper();
			}
			
			return _instance;
		}

        public static function reloadSheetConfig():void
        {
            sheetConfig = loadJsonFile('sheet_config.json');
        }

        public static function loadJsonFile(path:String):Object
        {
            var file:File=File.userDirectory.resolvePath(path);
            if(file.exists)
            {
                var fs:FileStream=new FileStream();
                fs.open(file, FileMode.READ);
                var jsonStr:String=fs.readUTFBytes(fs.bytesAvailable);
                var jsonObj:Object=JSON.parse(jsonStr);

                fs.close();

                return jsonObj;
            }

            return null;
        }
		
		public static function dump(mc:DisplayObject):void
		{
			if(mc is DisplayObjectContainer)
			{
				var doc:DisplayObjectContainer=mc as DisplayObjectContainer;
				for(var i:int=0;i<doc.numChildren;i++)
				{
					Console.log('container '+doc.name + '  '+ doc.toString());
					dump(doc.getChildAt(i));
				}
			}else{
				Console.log(mc.name + '  '+ mc.toString());
			}
		}
		public static function numberEqual(num1:Number,num2:Number):Boolean
		{
			if(Math.abs(num1-num2)<0.001)
			{
				return true;
			}else{
				return false;
			}
		}
		public static function platform():String
		{
			return Capabilities.os.toLowerCase().substr(0,3);
		}
		
		public static function nativeCall(args:Vector.<String>=null,onOutput:Function=null,onError:Function=null,onExit:Function=null):void
		{	
			var npsi:NativeProcessStartupInfo=new NativeProcessStartupInfo();
			npsi.executable=File.applicationDirectory.resolvePath("/bin/bash");
			npsi.arguments=args;
			
			var process:NativeProcess=new NativeProcess();
			
			if(onOutput!=null)
			{
				process.addEventListener(ProgressEvent.STANDARD_OUTPUT_DATA,onOutput);
			}
			if(onError!=null)
			{
				process.addEventListener(ProgressEvent.STANDARD_ERROR_DATA,onError);
			}
			if(onExit!=null)
			{
				process.addEventListener(NativeProcessExitEvent.EXIT, onExit);
			}
			
			process.addEventListener(ProgressEvent.STANDARD_ERROR_DATA, onErrorData);
			process.addEventListener(IOErrorEvent.STANDARD_OUTPUT_IO_ERROR, onIOError);
			process.addEventListener(IOErrorEvent.STANDARD_ERROR_IO_ERROR, onIOError);
			
			process.start(npsi);
		}
		
		public static function onErrorData(event:ProgressEvent):void
		{
			var process:NativeProcess=event.target as NativeProcess;
			Console.error(process.standardError.readUTFBytes(process.standardError.bytesAvailable)); 
		}
		public static function onIOError(event:IOErrorEvent):void
		{
			Console.error(event.toString());
		}
		
		public static function saveStringToFile(str:String,fileName:String,path:String=null):void
		{
			var bytearray:ByteArray=new ByteArray();
			bytearray.writeUTFBytes(str);
			
			Helper.saveBinaryToFile(bytearray,fileName,path);
		}
		public static function saveBinaryToFile(byteArray:ByteArray,fileName:String,path:String=null):void
		{
			var fs:FileStream=new FileStream();
			var file:File=File.userDirectory.resolvePath(fileName);
			fs.open(file,FileMode.WRITE);
			fs.writeBytes(byteArray);
			fs.close();
		}
		
	}
}