package fitnesse.responders.run;

import fitnesse.FitNesseContext;
import fitnesse.html.template.HtmlPage;
import fitnesse.html.template.PageTitle;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.responders.ChunkingResponder;
import fitnesse.testrunner.SuiteContentsFinder;
import fitnesse.testrunner.SuiteFilter;
import fitnesse.testrunner.run.PagePositions;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;

import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responder to display the tests that would be executed, in which order, for a given suite.
 */
public class PartitionPreviewResponder extends ChunkingResponder {

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    Response response = super.makeResponse(context, request);
    if (request.hasInput("html")) {
      response.setContentType("text/html");
    } else {
      response.setContentType("text/tab-separated-values");
    }
    return response;
  }

  @Override
  protected void doSending() throws Exception {
    PagePositions pages = getPagesToRun();
    try {
      if (request.hasInput("html")) {
        makePartitionPreviewHtmlResponse().render(response.getWriter(), request);
      } else {
        Writer writer = response.getWriter();
        pages.appendTo(writer, "\t");
      }
    } finally {
      response.close();
    }
  }

  protected PagePositions getPagesToRun() {
    SuiteFilter filter = SuiteResponder.createSuiteFilter(request, page.getFullPath().toString());
    SuiteContentsFinder suiteTestFinder = new SuiteContentsFinder(page, filter, root);
    List<WikiPage> allPages = suiteTestFinder.getAllPagesToRunForThisSuite();
    return applyPartition(allPages);
  }

  protected PagePositions applyPartition(List<WikiPage> pages) {
    return context.testRunFactoryRegistry.findPagePositions(pages);
  }

  private HtmlPage makePartitionPreviewHtmlResponse() throws UnsupportedEncodingException {
    HtmlPage page = context.pageFactory.newPage();
    page.setTitle("Partitioning preview");
    page.setPageTitle(new PageTitle(PathParser.parse(request.getResource())));
    page.setNavTemplate("viewNav");
    page.put("partitionCount", getPartitionCount());
    page.put("partitioning", getPagesToRun());
    page.setMainTemplate("partitionPreview");

    return page;
  }

  private int getPartitionCount() {
    String qs = request.getQueryString();
    Pattern partitionCountPattern = Pattern.compile("partitionCount=([0-9]+)");
    Matcher matcher = partitionCountPattern.matcher(qs);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }
    return 0;
  }
}
