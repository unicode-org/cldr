<%@tag description="Base page template" pageEncoding="UTF-8"%>
<%@attribute name="info" fragment="true" %>
<%@attribute name="footer" fragment="true" %>
<%@attribute name="css" fragment="true" %>
<%@attribute name="script" fragment="true" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">

    <title>Test login UI</title>

    <!-- Bootstrap core test CSS -->
    <link href="../css/bootstrap.min.css" rel="stylesheet">

    <!-- Custom styles for this template -->
    <link href="../css/redesign.css" rel="stylesheet">

    <jsp:invoke fragment="css"/>
c
    <!-- Just for debugging purposes. Don't actually copy this line! -->
    <!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

    <!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
      <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->
  </head>

  <body data-spy="scroll" data-target="#navspy">

    <div class="navbar navbar-fixed-top" role="navigation">
      <div class="container-fluid">
        <div class="navbar-header">
          <a class="navbar-brand" href="#">Welcome</a>
        </div>
        <div class="collapse navbar-collapse">
          <ul class="nav navbar-nav">
            <li class="active"><a href="#">Survey Tools</a></li>
            <li><a href="#">Manage</a></li>
            <li><a href="#about">Informations</a></li>
            <li class="dropdown">
                      <a href="#" class="dropdown-toggle" data-toggle="dropdown">Coverage <b class="caret"></b></a>
                      <ul class="dropdown-menu">
                        <li><a href="#">Action</a></li>
                        <li><a href="#">Another action</a></li>
                        <li><a href="#">Something else here</a></li>
                        <li class="divider"></li>
                        <li><a href="#">Separated link</a></li>
                      </ul>
           </li>
          </ul>
            
          <p class="navbar-text navbar-right"><nyname.blu38aeu0@odjk.apple.example.com> nyname_TESTER_ Apple (<a href="#about" class="navbar-link">Logout</a>)</p>
          
        </div>
      </div>
    </div>

    <!-- /.container-fluid -->
    <div class="container-fluid">
        <div class="row menu-position">
            <div class="col-md-9">
                <ol class="breadcrumb" style="background-color: white">
                    <li><a href="#">Survey Tools 25</a></li>
                    <li><a href="#">French (Benin)</a></li>
                    <li class="active">
                      <div class="btn-group">
                        <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown">
                          Action <span class="caret"></span>
                        </button>
                        <ul class="dropdown-menu" role="menu">
                          <li><a href="#">Action</a></li>
                          <li><a href="#">Another action</a></li>
                          <li><a href="#">Something else here</a></li>
                          <li class="divider"></li>
                          <li><a href="#">Separated link</a></li>
                        </ul>
                      </div>
                    </li>
                    <li>
                      <div class="btn-group">
                        <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown">
                          Action <span class="caret"></span>
                        </button>
                        <ul class="dropdown-menu" role="menu">
                          <li><a href="#">Action</a></li>
                          <li><a href="#">Another action</a></li>
                          <li><a href="#">Something else here</a></li>
                          <li class="divider"></li>
                          <li><a href="#">Separated link</a></li>
                        </ul>
                      </div>
                    </li>
                </ol>
            <jsp:invoke fragment="info"/>
            <%--<div class="alert info-warning alert-info">
                <span class="glyphicon glyphicon-info-sign"></span>
                        Welcome on board ! Choose your local to start !
                        <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
            </div>--%>
            </div>
            <div class="col-md-3" style="padding: 8px">
                <div class="btn-group">
                        <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown">
                          Review <span class="caret"></span>
                        </button>
                        <ul class="dropdown-menu" role="menu">
                          <li><a href="#">Action</a></li>
                          <li><a href="#">Another action</a></li>
                          <li><a href="#">Something else here</a></li>
                          <li class="divider"></li>
                          <li><a href="#">Separated link</a></li>
                        </ul>
               </div>
            </div>
            
        </div>
        
    <div class="row" style="padding-top:220px">
    
    		<jsp:doBody/>
            <%--<div class="col-md-12">
                <div class="input-group input-group-lg">
                    <input type="text" class="form-control" placeholder="Local : French"/>
                        <span class="input-group-addon"><span class="glyphicon glyphicon-search"></span></span>
                </div>
            </div>--%>
        </div>
    </div>
    
  
    <!-- Bootstrap core JavaScript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
    <script src="../js/bootstrap.min.js"></script>
    <script src="../js/redesign.js"></script>
    <jsp:invoke fragment="script"/>

</body>
</html>
