//>>built
require({cache:{"url:dijit/templates/Dialog.html":"<div class=\"dijitDialog\" role=\"dialog\" aria-labelledby=\"${id}_title\">\n\t<div data-dojo-attach-point=\"titleBar\" class=\"dijitDialogTitleBar\">\n\t\t<span data-dojo-attach-point=\"titleNode\" class=\"dijitDialogTitle\" id=\"${id}_title\"\n\t\t\t\trole=\"heading\" level=\"1\"></span>\n\t\t<span data-dojo-attach-point=\"closeButtonNode\" class=\"dijitDialogCloseIcon\" data-dojo-attach-event=\"ondijitclick: onCancel\" title=\"${buttonCancel}\" role=\"button\" tabIndex=\"-1\">\n\t\t\t<span data-dojo-attach-point=\"closeText\" class=\"closeText\" title=\"${buttonCancel}\">x</span>\n\t\t</span>\n\t</div>\n\t<div data-dojo-attach-point=\"containerNode\" class=\"dijitDialogPaneContent\"></div>\n</div>\n"}});
define("dijit/Dialog",["require","dojo/_base/array","dojo/_base/connect","dojo/_base/declare","dojo/_base/Deferred","dojo/dom","dojo/dom-class","dojo/dom-geometry","dojo/dom-style","dojo/_base/event","dojo/_base/fx","dojo/i18n","dojo/keys","dojo/_base/lang","dojo/on","dojo/ready","dojo/sniff","dojo/window","dojo/dnd/Moveable","dojo/dnd/TimedMoveable","./focus","./_base/manager","./_Widget","./_TemplatedMixin","./_CssStateMixin","./form/_FormMixin","./_DialogMixin","./DialogUnderlay","./layout/ContentPane","dojo/text!./templates/Dialog.html","./main","dojo/i18n!./nls/common"],function(_1,_2,_3,_4,_5,_6,_7,_8,_9,_a,fx,_b,_c,_d,on,_e,_f,_10,_11,_12,_13,_14,_15,_16,_17,_18,_19,_1a,_1b,_1c,_1d){
var _1e=_4("dijit._DialogBase",[_16,_18,_19,_17],{templateString:_1c,baseClass:"dijitDialog",cssStateNodes:{closeButtonNode:"dijitDialogCloseIcon"},_setTitleAttr:[{node:"titleNode",type:"innerHTML"},{node:"titleBar",type:"attribute"}],open:false,duration:_14.defaultDuration,refocus:true,autofocus:true,_firstFocusItem:null,_lastFocusItem:null,doLayout:false,draggable:true,_setDraggableAttr:function(val){
this._set("draggable",val);
},"aria-describedby":"",maxRatio:0.9,postMixInProperties:function(){
var _1f=_b.getLocalization("dijit","common");
_d.mixin(this,_1f);
this.inherited(arguments);
},postCreate:function(){
_9.set(this.domNode,{display:"none",position:"absolute"});
this.ownerDocumentBody.appendChild(this.domNode);
this.inherited(arguments);
this.connect(this,"onExecute","hide");
this.connect(this,"onCancel","hide");
this._modalconnects=[];
},onLoad:function(){
this._position();
if(this.autofocus&&_20.isTop(this)){
this._getFocusItems(this.domNode);
_13.focus(this._firstFocusItem);
}
this.inherited(arguments);
},_onBlur:function(by){
this.inherited(arguments);
var _21=_d.hitch(this,function(){
if(this.open&&!this._destroyed&&_20.isTop(this)){
this._getFocusItems(this.domNode);
_13.focus(this._firstFocusItem);
}
});
if(by=="mouse"){
on.once(this.ownerDocument,"mouseup",_21);
}else{
_21();
}
},_endDrag:function(){
var _22=_8.position(this.domNode),_23=_10.getBox(this.ownerDocument);
_22.y=Math.min(Math.max(_22.y,0),(_23.h-_22.h));
_22.x=Math.min(Math.max(_22.x,0),(_23.w-_22.w));
this._relativePosition=_22;
this._position();
},_setup:function(){
var _24=this.domNode;
if(this.titleBar&&this.draggable){
this._moveable=new ((_f("ie")==6)?_12:_11)(_24,{handle:this.titleBar});
this.connect(this._moveable,"onMoveStop","_endDrag");
}else{
_7.add(_24,"dijitDialogFixed");
}
this.underlayAttrs={dialogId:this.id,"class":_2.map(this["class"].split(/\s/),function(s){
return s+"_underlay";
}).join(" "),ownerDocument:this.ownerDocument};
},_size:function(){
this._checkIfSingleChild();
if(this._singleChild){
if(typeof this._singleChildOriginalStyle!="undefined"){
this._singleChild.domNode.style.cssText=this._singleChildOriginalStyle;
delete this._singleChildOriginalStyle;
}
}else{
_9.set(this.containerNode,{width:"auto",height:"auto"});
}
var bb=_8.position(this.domNode);
var _25=_10.getBox(this.ownerDocument);
_25.w*=this.maxRatio;
_25.h*=this.maxRatio;
if(bb.w>=_25.w||bb.h>=_25.h){
var _26=_8.position(this.containerNode),w=Math.min(bb.w,_25.w)-(bb.w-_26.w),h=Math.min(bb.h,_25.h)-(bb.h-_26.h);
if(this._singleChild&&this._singleChild.resize){
if(typeof this._singleChildOriginalStyle=="undefined"){
this._singleChildOriginalStyle=this._singleChild.domNode.style.cssText;
}
this._singleChild.resize({w:w,h:h});
}else{
_9.set(this.containerNode,{width:w+"px",height:h+"px",overflow:"auto",position:"relative"});
}
}else{
if(this._singleChild&&this._singleChild.resize){
this._singleChild.resize();
}
}
},_position:function(){
if(!_7.contains(this.ownerDocumentBody,"dojoMove")){
var _27=this.domNode,_28=_10.getBox(this.ownerDocument),p=this._relativePosition,bb=p?null:_8.position(_27),l=Math.floor(_28.l+(p?p.x:(_28.w-bb.w)/2)),t=Math.floor(_28.t+(p?p.y:(_28.h-bb.h)/2));
_9.set(_27,{left:l+"px",top:t+"px"});
}
},_onKey:function(evt){
if(evt.charOrCode){
var _29=evt.target;
if(evt.charOrCode===_c.TAB){
this._getFocusItems(this.domNode);
}
var _2a=(this._firstFocusItem==this._lastFocusItem);
if(_29==this._firstFocusItem&&evt.shiftKey&&evt.charOrCode===_c.TAB){
if(!_2a){
_13.focus(this._lastFocusItem);
}
_a.stop(evt);
}else{
if(_29==this._lastFocusItem&&evt.charOrCode===_c.TAB&&!evt.shiftKey){
if(!_2a){
_13.focus(this._firstFocusItem);
}
_a.stop(evt);
}else{
while(_29){
if(_29==this.domNode||_7.contains(_29,"dijitPopup")){
if(evt.charOrCode==_c.ESCAPE){
this.onCancel();
}else{
return;
}
}
_29=_29.parentNode;
}
if(evt.charOrCode!==_c.TAB){
_a.stop(evt);
}else{
if(!_f("opera")){
try{
this._firstFocusItem.focus();
}
catch(e){
}
}
}
}
}
}
},show:function(){
if(this.open){
return;
}
if(!this._started){
this.startup();
}
if(!this._alreadyInitialized){
this._setup();
this._alreadyInitialized=true;
}
if(this._fadeOutDeferred){
this._fadeOutDeferred.cancel();
}
var win=_10.get(this.ownerDocument);
this._modalconnects.push(on(win,"scroll",_d.hitch(this,"resize")));
this._modalconnects.push(on(this.domNode,_3._keypress,_d.hitch(this,"_onKey")));
_9.set(this.domNode,{opacity:0,display:""});
this._set("open",true);
this._onShow();
this._size();
this._position();
var _2b;
this._fadeInDeferred=new _5(_d.hitch(this,function(){
_2b.stop();
delete this._fadeInDeferred;
}));
_2b=fx.fadeIn({node:this.domNode,duration:this.duration,beforeBegin:_d.hitch(this,function(){
_20.show(this,this.underlayAttrs);
}),onEnd:_d.hitch(this,function(){
if(this.autofocus&&_20.isTop(this)){
this._getFocusItems(this.domNode);
_13.focus(this._firstFocusItem);
}
this._fadeInDeferred.resolve(true);
delete this._fadeInDeferred;
})}).play();
return this._fadeInDeferred;
},hide:function(){
if(!this._alreadyInitialized||!this.open){
return;
}
if(this._fadeInDeferred){
this._fadeInDeferred.cancel();
}
var _2c;
this._fadeOutDeferred=new _5(_d.hitch(this,function(){
_2c.stop();
delete this._fadeOutDeferred;
}));
this._fadeOutDeferred.then(_d.hitch(this,"onHide"));
_2c=fx.fadeOut({node:this.domNode,duration:this.duration,onEnd:_d.hitch(this,function(){
this.domNode.style.display="none";
_20.hide(this);
this._fadeOutDeferred.resolve(true);
delete this._fadeOutDeferred;
})}).play();
if(this._scrollConnected){
this._scrollConnected=false;
}
var h;
while(h=this._modalconnects.pop()){
h.remove();
}
if(this._relativePosition){
delete this._relativePosition;
}
this._set("open",false);
return this._fadeOutDeferred;
},resize:function(){
if(this.domNode.style.display!="none"){
if(_1a._singleton){
_1a._singleton.layout();
}
this._position();
this._size();
}
},destroy:function(){
if(this._fadeInDeferred){
this._fadeInDeferred.cancel();
}
if(this._fadeOutDeferred){
this._fadeOutDeferred.cancel();
}
if(this._moveable){
this._moveable.destroy();
}
var h;
while(h=this._modalconnects.pop()){
h.remove();
}
_20.hide(this);
this.inherited(arguments);
}});
var _2d=_4("dijit.Dialog",[_1b,_1e],{});
_2d._DialogBase=_1e;
var _20=_2d._DialogLevelManager={_beginZIndex:950,show:function(_2e,_2f){
ds[ds.length-1].focus=_13.curNode;
var _30=_1a._singleton;
if(!_30||_30._destroyed){
_30=_1d._underlay=_1a._singleton=new _1a(_2f);
}else{
_30.set(_2e.underlayAttrs);
}
var _31=ds[ds.length-1].dialog?ds[ds.length-1].zIndex+2:_2d._DialogLevelManager._beginZIndex;
if(ds.length==1){
_30.show();
}
_9.set(_1a._singleton.domNode,"zIndex",_31-1);
_9.set(_2e.domNode,"zIndex",_31);
ds.push({dialog:_2e,underlayAttrs:_2f,zIndex:_31});
},hide:function(_32){
if(ds[ds.length-1].dialog==_32){
ds.pop();
var pd=ds[ds.length-1];
if(!_1a._singleton._destroyed){
if(ds.length==1){
_1a._singleton.hide();
}else{
_9.set(_1a._singleton.domNode,"zIndex",pd.zIndex-1);
_1a._singleton.set(pd.underlayAttrs);
}
}
if(_32.refocus){
var _33=pd.focus;
if(pd.dialog&&(!_33||!_6.isDescendant(_33,pd.dialog.domNode))){
pd.dialog._getFocusItems(pd.dialog.domNode);
_33=pd.dialog._firstFocusItem;
}
if(_33){
try{
_33.focus();
}
catch(e){
}
}
}
}else{
var idx=_2.indexOf(_2.map(ds,function(_34){
return _34.dialog;
}),_32);
if(idx!=-1){
ds.splice(idx,1);
}
}
},isTop:function(_35){
return ds[ds.length-1].dialog==_35;
}};
var ds=_2d._dialogStack=[{dialog:null,focus:null,underlayAttrs:null}];
if(_f("dijit-legacy-requires")){
_e(0,function(){
var _36=["dijit/TooltipDialog"];
_1(_36);
});
}
return _2d;
});
