package searchengine.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.Website;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.UserAgent;
import searchengine.dto.statistics.PageData;
import searchengine.repository.SiteRepository;
import searchengine.services.impl.IndexingServiceImpl;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class LinksSearch extends RecursiveTask<List<PageData>> {
    private final String mainLink;
    private final String mainLinkV2;
    private final String link;
    private final List<String> linkList;
    private final List<PageData> pageDataList;
    private final SiteRepository siteRepository;

    public LinksSearch(String mainLink, String mainLinkV2, String link, List<String> linkList, List<PageData> pageDataList, SiteRepository siteRepository){
        this.mainLink = mainLink;
        this.mainLinkV2 = mainLinkV2;
        this.link = link;
        this.linkList = linkList;
        this.pageDataList = pageDataList;
        this.siteRepository = siteRepository;
    }

    public Document getConnectWithUserAgent(String url){
        Document document = null;
        try{
            Thread.sleep(100);
            document = Jsoup.connect(url).userAgent(UserAgent.getUserAgent()).referrer("http://www.google.com").get();
        }catch (Exception ex){
            log.debug("Не удается подключится к сайту " + url);
        }
        return document;
    }
    public Document getConnect(String url){
        Document document = null;
        try{
            document = Jsoup.connect(url).get();
        } catch (Exception e) {
            log.debug("Не удается подключится к сайту " + url);
        }
        return document;
    }

    @SneakyThrows
    @Override
    protected List<PageData> compute() {
        if (!Thread.interrupted() && !IndexingServiceImpl.isInterapted) {
            try {
                Thread.sleep(100);
                updateDate();
                Document document;
                String html;
                try {
                    document = getConnectWithUserAgent(link);
                    html = document.outerHtml();
                } catch (Exception e){
                    document = getConnect(link);
                    html = document.outerHtml();
                }
                Connection.Response response = document.connection().response();
                int statusCode = response.statusCode();
                PageData pageData = new PageData(link, html, statusCode);
                pageDataList.add(pageData);
                Elements elements = document.select("body").select("a");
                List<LinksSearch> taskList = new ArrayList<>();
                for (Element element : elements) {
                    String elem = element.attr("abs:href");
                    if ((elem.startsWith(mainLink) || elem.startsWith(mainLinkV2)) && !elem.contains("#") && !elem.contains(".jpg") && !elem.contains(".png") && !elem.contains(".pdf") && !linkList.contains(elem)) {
                        linkList.add(elem);
                        LinksSearch task = new LinksSearch(mainLink, mainLinkV2, elem, linkList, pageDataList, siteRepository);
                        task.fork();
                        taskList.add(task);
                    }
                }
                taskList.forEach(ForkJoinTask::join);
            } catch (Exception ex) {
                log.debug("Ошибка парсинга: " + link);
                PageData pageData = new PageData(link, "", 500);
                pageDataList.add(pageData);
            }
        } else {
            throw new InterruptedException();
        }
        return pageDataList;
    }
    private void updateDate(){
        Website website = siteRepository.findByUrl(mainLink);
        website.setStatusTime(new Date());
        siteRepository.flush();
        siteRepository.save(website);
    }
}
