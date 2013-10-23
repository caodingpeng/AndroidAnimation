package com.juicebeer.elements
{	
	import com.juicebeer.component.Console;
	
	import flash.display.DisplayObject;
	import flash.display.DisplayObjectContainer;
	import flash.display.MovieClip;
	import flash.filesystem.File;
	
	public class SpriteElement extends ElementBase
	{
		
		public var children:Array;
		public function SpriteElement(obj:DisplayObject)
		{
			super(obj);
			
			children=new Array();
			this.exportChilds();
		}
		
		override public function get type():String
		{
			return "sprite";
		}
		
		private function exportChilds():void
		{
			var mc:MovieClip=this._obj as MovieClip;
			
			for(var i:int=0;i<mc.numChildren;i++)
			{
				var child:DisplayObject=mc.getChildAt(i);
				if(child.name)
				{
					var type:String=child.name.substring(0,child.name.indexOf('_'));
					
					switch(type)
					{
						case 'sprite':
							var sprite:SpriteElement=new SpriteElement(child);
							this.children.push(sprite);
							break;
						case 'mc':
							var mce:MovieClipElement=new MovieClipElement(child);
							this.children.push(mce);
							break;
                        case "ignore":
                            break;
						default:
							break;
					}
				}
			}
		}
	}
}