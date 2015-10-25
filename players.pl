use strict;

mkdir('stats');

my $MAX_CNT=1000000;

my $reference_year=2015;

my @listed_fields=get_listed_fields();

my $terms=read_terms();

my $current_term='';

my $command='';

sub read_terms
{
	my $terms={};
	open(TERMS,"terms.txt");
	map
	{ 
		chomp($_);
		my @terms=split /\t/,$_;
		my $name=$terms[0];
		if($name ne '')
		{
			$terms->{$name}=$terms[1];
		}
	} <TERMS>;
	close(TERMS);
	return $terms;
}

sub list_terms
{
	join("\n",map { "$_\t$terms->{$_}" } keys(%{$terms}));
}

my $result;

while(!($command=~/x/i))
{

	print "\n\n---------------------------\n\nlisted fields:\n\n",join("\t",@listed_fields),"\n\n";

	print qq(
x = exit

startup:

s = startup ( ck + cl + as )

preprocessing:

ck = count keys
cl = create list

prerequisite processing:

as = age stats

subsequent processing:

v = values
pr = proper records
cs = country stats
csa = country stats ( 20 - 40 )
yt = young talents ( 10 - 30 )
ot = old talents ( 60 - 100 )
sd = standard deviation
hm = high rated men ( >=2600 )
hw = high rated women ( >=2300 )
h = high rated players ( >=2500 )
ga = GM average ratings ( 20 - 40 )
yp = young players ( 5 - 6 )
pwg = players without gender

d = define term
l = list terms
s [name] = search term
ct = current term ( $current_term )

$result

enter command: );
	$command=<>;
	chomp($command);
	
	print "\n";
	
	if($command=~/^v$/i)
	{
	
		my $fields={};
		
		my @field_list=qw(flag country sex birthday);
		
		my $cnt=0;
	
		iterate(sub
		{
		
			my ($record)=@_;
			
			map
			{
				$fields->{$_}->{$record->{$_}}++;
			}
			@field_list;
			
			$cnt++;
			
			if(($cnt%10000)==0)
			{
				print "$cnt processed\n";
			}
		
		}
		);
		
		my $stats='';
		
		foreach(keys(%{$fields}))
		{
		
			my $key=$_;
			
			$stats.="$key\n";
			my @values=keys(%{$fields->{$key}});
			
			@values=sort @values;
			
			$stats.=join("\n",map { "$_\t$fields->{$key}->{$_}" } @values)."\n";
			
		}
		
		save("fieldvalues",$stats,"field\tvalue");
	
	}
	
	if($command=~/^d$/i)
	{
	
		print "name: ";my $name=<>;chomp($name);
		print "term: ";my $term=<>;chomp($term);
		
		$terms->{$name}=$term;
		
		$result=list_terms();
		
		open(TERMS,">terms.txt");
		print TERMS $result;
		close(TERMS);
		
	}
	
	if($command=~/^l$/i)
	{
		$result=list_terms();
	}
	
	if($command=~/^s ([A-Za-z0-9]+)$/i)
	{
		my $name=$1;
		
		iterate_filtered($name);
	}
	
	if($command=~/^ct ([A-Za-z0-9]+)$/i)
	{
		my $name=$1;
		
		if($name eq 'p')
		{
			$current_term='';
		}
		else
		{		
			$current_term=$1;
		}
	}
	
	if($command=~/^s$/i)
	{
		preprocess('count keys');
		print "\n";
		preprocess('create list');
		print "\ncalculating age stats\n";
		age_stats();
	}
	
	if($command=~/^ck$/i)
	{
		preprocess('count keys');
	}
	
	if($command=~/^cl$/i)
	{
		preprocess('create list');
	}
	
	if($command=~/as/i)
	{
		age_stats();
	}
	
	if($command=~/^cs$/i)
	{
		country_stats(1,100);
	}
	
	if($command=~/^csa$/i)
	{
		country_stats(20,40);
	}
	
	if($command=~/^pr$/i)
	{
		proper();
	}
	
	if($command=~/yt/i)
	{
		young_talents(10,30,'young',50);
	}
	
	if($command=~/sd/i)
	{
		young_talents(1,100,'std_dev',1000);
	}

	if($command=~/ot/i)
	{
		young_talents(60,100,'old',100);
	}
	
	if($command=~/hm/i)
	{
		high_rated_players("M",2600);
	}
	
	if($command=~/hw/i)
	{
		high_rated_players("F",2300);
	}
	
	if($command=~/h$/i)
	{
		high_rated_players("M|F",2500);
	}
	
	if($command=~/ga/i)
	{
		gm_average_ratings();
	}
	
	if($command=~/yp/i)
	{
		young_players();
	}
	
	if($command=~/pwg/i)
	{
		players_without_gender();
	}
	
}

sub get_listed_fields
{

	open(KEY_COUNTS,"stats/key_counts.txt");
	
	<KEY_COUNTS>;
	<KEY_COUNTS>;
	
	my @keys=();
	
	while(<KEY_COUNTS>)
	{
		my $line=$_;
		
		chomp($line);
		
		my @line=split /\t/,$line;
		
		my $key=$line[0];
		
		push(@keys,$key);
	}	
	
	close(KEY_COUNTS);
	
	return @keys;
	
}

sub young_players
{

	my $young_txt='';

	iterate(sub {
		my $record=shift;
		
		if(($record->{age}>=4)&&($record->{age}<=6)&&($record->{rating}>0)&&($record->{sex}=~/M|F/))
		{
			
			$young_txt.="$record->{line}\n";
			
		}
		
	});
	
	print $young_txt;
	
	save("young_players",$young_txt,join("\t",@listed_fields));

}


sub players_without_gender
{

	my $without_gender_txt='';
	
	my $cnt=0;

	iterate(sub {
		my $record=shift;
		
		if(!($record->{sex}=~/M|F/))
		{
			
			$without_gender_txt.="$record->{line}\n";
			
			$cnt++;
			
		}
		
	});
	
	$without_gender_txt.="$cnt\n";
	
	print $without_gender_txt;
	
	save("without_gender",$without_gender_txt,join("\t",@listed_fields));

}



sub young_talents
{
	
	my ($from,$to,$name,$size)=@_;

	open(AGE_STATS,"stats/age_stats.txt");
	
	my $age_stats={};
	
	<AGE_STATS>;
	
	while(<AGE_STATS>)
	{
		chomp $_;
		my @fields=split /\t/,$_;
		
		my $age=$fields[0];
		my $RM=$fields[2];
		my $AVGRM=$fields[3];
		my $RF=$fields[4];
		my $AVGRF=$fields[5];
		
		$age_stats->{$age}->{RM}=$RM;
		$age_stats->{$age}->{AVGRM}=$AVGRM;
		$age_stats->{$age}->{RF}=$RF;
		$age_stats->{$age}->{AVGRF}=$AVGRF;
	}
	
	close(AGE_STATS);
	
	my $list={};
	
	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>0)&&($record->{sex}=~/M|F/)
		&&($record->{age}>=$from)&&($record->{age}<=$to))
		{
			my $expected_rating=$age_stats->{$record->{age}}->{"AVGR$record->{sex}"};
			my $rating_surplus=$record->{rating}-$expected_rating;
			
			$list->{"$record->{name}\t$record->{country}\t$record->{sex}\t$record->{age}\t".int($expected_rating)."\t$record->{rating}"}=$rating_surplus;
			
			
			$age_stats->{$record->{age}}->{"DEVSQ$record->{sex}"}+=
			$rating_surplus*$rating_surplus;
			
			$age_stats->{TOT}->{"DEVSQ$record->{sex}"}+=
			$rating_surplus*$rating_surplus;
			
			$age_stats->{TOT}->{"R$record->{sex}"}++;
			
			
		}
	});
	
	my @talents=keys %{$list};
	
	@talents=sort
	{
		$list->{$b}<=>$list->{$a};
	}
	@talents;
	
	my $young_talents_txt='';
	
	for(my $i=0;$i<$size;$i++)
	{
		my $index=$i+1;
		my $item="$index. $talents[$i]\t".int($list->{$talents[$i]});
		
		$young_talents_txt.="$item\n";
	}
	
	print $young_talents_txt;
	
	save($name."_talents",$young_talents_txt,"name\tcountry\tgender\tage\texpected rating\tactual rating\trating surplus");
	
	my $std_dev_txt='';
	
	my $TOTDEVSQM=$age_stats->{TOT}->{DEVSQM};
	my $TOTRM=$age_stats->{TOT}->{RM};
	my $TOTAVGDEVSQM=ratio($TOTDEVSQM,$TOTRM);
	my $TOTSTDDEVM=sprintf "%.1f",sqrt($TOTAVGDEVSQM);
	my $TOTDEVSQF=$age_stats->{TOT}->{DEVSQF};
	my $TOTRF=$age_stats->{TOT}->{RF};
	my $TOTAVGDEVSQF=ratio($TOTDEVSQF,$TOTRF);
	my $TOTSTDDEVF=sprintf "%.1f",sqrt($TOTAVGDEVSQF);
	
	for(my $age=$from;$age<=$to;$age++)
	{
	
		my $DEVSQM=$age_stats->{$age}->{DEVSQM};
		my $RM=$age_stats->{$age}->{RM};
		my $AVGDEVSQM=ratio($DEVSQM,$RM);
		my $STDDEVM=$AVGDEVSQM>0?sprintf "%.1f",sqrt($AVGDEVSQM):'N/A';
		my $DEVSQF=$age_stats->{$age}->{DEVSQF};
		my $RF=$age_stats->{$age}->{RF};
		my $AVGDEVSQF=ratio($DEVSQF,$RF);
		my $STDDEVF=$AVGDEVSQF>0?sprintf "%.1f",sqrt($AVGDEVSQF):'N/A';
		
		my $age_field=($age%5==0?$age:'');
		
		$std_dev_txt.="$age_field\t$STDDEVM\t$STDDEVF\n";
	
	}
	
	$std_dev_txt.="TOT\t$TOTSTDDEVM\t$TOTSTDDEVF\n";
	
	save("std_dev_$from"."_"."$to",$std_dev_txt,"age\tstd dev male\tstd dev female");
	
}

sub ratio
{
	my ($cumulative,$count)=@_;
	
	if($count>0)
	{
		return $cumulative/$count;
	}
	
	return 'N/A';
}

sub averagef
{
	my ($cumulative,$count)=@_;
	
	if($count>0)
	{
		return sprintf "%.1f",$cumulative/$count;
	}
	
	return 'N/A';
}

sub percentf
{
	my ($ratio)=@_;
	
	if($ratio ne 'N/A')
	{
		return sprintf "%.2f",$ratio*100;
	}
	
	return 'N/A';
}

sub save
{
	my ($file_name,$content,$html_headers)=@_;
	
	$content="$html_headers\n$content";
	
	open(TXT,">stats/$file_name.txt");
	print TXT $content;
	close(TXT);
	
	my @content=split /\n/,$content;
		
	my $html_content="<table border=1 cellpadding=3 cellspacing=3>";
	
	foreach(@content)
	{
	
		my @fields=split /\t/,$_;
		
		my $item=join('',map { "<td align=center>$_</td>"; } @fields);
		
		$html_content.="<tr>$item</tr>\n";
	
	}
	
	$html_content.="</table>";
	
	open(HTML,">stats/$file_name.html");
	print HTML $html_content;
	close(HTML);
	
}

sub age_stats
{
	
	my $age_stats={};
	
	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>0)&&($record->{sex}=~/M|F/)&&($record->{age} ne ''))
		{
			$age_stats->{$record->{age}}->{"R$record->{sex}"}++;
			$age_stats->{$record->{age}}->{"CR$record->{sex}"}+=$record->{rating};
			
			if($record->{title} eq 'GM')
			{
				$age_stats->{$record->{age}}->{"GM$record->{sex}"}++;
			}
		}
	});
	
	my $age_stats_txt='';
	
	for(my $i=1;$i<=100;$i++)
	{
	
		my $RM=$age_stats->{$i}->{RM};
		my $AVGRM=averagef($age_stats->{$i}->{CRM},$RM);
		my $GMM=$age_stats->{$i}->{GMM};
		my $RF=$age_stats->{$i}->{RF};
		my $AVGRF=averagef($age_stats->{$i}->{CRF},$RF);
		my $GMF=$age_stats->{$i}->{GMF};
		
		my $age_field=($i%5==0?$i:'');
		
		my $item="$i\t$age_field\t$RM\t$AVGRM\t$RF\t$AVGRF\t$GMM\t$GMF";
		
		$age_stats_txt.="$item\n";
	
	}
	
	print $age_stats_txt;
	
	save("age_stats",$age_stats_txt,"age\tage in 5 year steps\trated males\taverage rating males\trated females\taverage rating females\tmale GMs\tfemale GMs");
	
}

sub country_stats
{

	my ($from,$to)=@_;
	
	my $country_stats={};
	
	my $all_stats={};
	
	iterate(sub {
		my $record=shift;
		
		if(
			($record->{sex}=~/M|F/)
			&&
			($record->{country} ne '')
			&&
			($record->{name} ne '')
			&&
			($record->{age}>=$from)
			&&
			($record->{age}<=$to)
			&&
			($record->{country} ne 'FID')
		)
		{
			$country_stats->{$record->{country}}->{"$record->{sex}"}++;
			if($record->{rating}>0)
			{
				$country_stats->{$record->{country}}->{"R$record->{sex}"}++;
				$country_stats->{$record->{country}}->{"CR$record->{sex}"}+=$record->{rating};
				
				$all_stats->{"RTOT$record->{sex}"}++;
				$all_stats->{"CRTOT$record->{sex}"}+=$record->{rating};
			}
		}
	});
	
	my $TOTAVGRM=ratio($all_stats->{CRTOTM},$all_stats->{RTOTM});
	my $TOTAVGRF=ratio($all_stats->{CRTOTF},$all_stats->{RTOTF});
	
	my $country_stats_txt='';
		
	my @countries=keys %{$country_stats};
	
	my @filtered_countries=();
	
	foreach(@countries)
	{
	
		my $country=$_;
		
		$country_stats->{$country}->{TOT}=
		$country_stats->{$country}->{M}+$country_stats->{$country}->{F};
		
		$country_stats->{$country}->{RTOT}=
		$country_stats->{$country}->{RM}+$country_stats->{$country}->{RF};
		
		$country_stats->{$country}->{FRATIO}=ratio($country_stats->{$country}->{F},$country_stats->{$country}->{TOT});
		
		$country_stats->{$country}->{FRATIOR}=ratio($country_stats->{$country}->{RF},$country_stats->{$country}->{RTOT});
		
		$country_stats->{$country}->{FPERCENT}=percentf($country_stats->{$country}->{FRATIO});
		
		$country_stats->{$country}->{FPERCENTR}=percentf($country_stats->{$country}->{FRATIOR});
		
		$country_stats->{$country}->{AVGRM}=averagef($country_stats->{$country}->{CRM},$country_stats->{$country}->{RM});
		
		$country_stats->{$country}->{AVGRF}=averagef($country_stats->{$country}->{CRF},$country_stats->{$country}->{RF});
		
		$country_stats->{$country}->{AVGRDIFF}=
			(($country_stats->{$country}->{AVGRM} ne 'N/A')&&($country_stats->{$country}->{AVGRF} ne 'N/A'))?sprintf "%.1f",$country_stats->{$country}->{AVGRM}-$country_stats->{$country}->{AVGRF}:'N/A';
		
		if($country_stats->{$country}->{RTOT}>100)
		{
		
			push(@filtered_countries,$_);
		
		}
		
	}
	
	@filtered_countries=sort
	{
		$country_stats->{$b}->{FRATIO}<=>$country_stats->{$a}->{FRATIO};
	}
	@filtered_countries;
	
	foreach(@filtered_countries)
	{
	
		my $country=$_;
	
		my $TOT=$country_stats->{$country}->{TOT};
		my $M=$country_stats->{$country}->{M};
		my $RM=$country_stats->{$country}->{RM};
		my $AVGRM=$country_stats->{$country}->{AVGRM};
		my $F=$country_stats->{$country}->{F};
		my $RF=$country_stats->{$country}->{RF};
		my $AVGRF=$country_stats->{$country}->{AVGRF};
		my $FPERCENT=$country_stats->{$country}->{FPERCENT};
		my $FPERCENTR=$country_stats->{$country}->{FPERCENTR};
		my $AVGRDIFF=$country_stats->{$country}->{AVGRDIFF};
		
		my $item="$country\t$TOT\t$M\t$RM\t$AVGRM\t$F\t$RF\t$AVGRF\t$FPERCENT\t$FPERCENTR\t$AVGRDIFF";
		
		$country_stats_txt.="$item\n";
	
	}
	
	$country_stats_txt.="totavgr male\t$TOTAVGRM\ttotavgr female\t$TOTAVGRF\n";
	
	print $country_stats_txt;
	
	save("country_stats_$from"."_"."$to",$country_stats_txt,"country\ttotal players\tmales\trated males\taverage rating males\tfemales\trated females\taverage rating females\tfemale % of all players\tfemale % of rated players\taverage rating difference");
	
}

sub proper
{

	my $proper=0;
	
	iterate(sub {
		my $record=shift;
		
		if(
			($record->{name} ne '')
			&&
			($record->{birthday}>1900)
			&&
			($record->{country} ne '')
			&&
			($record->{sex}=~/M|F/)
		)
		{
			$proper++;
		}
		
	});
	
	print "proper records: $proper\n";

}

sub high_rated_players
{

	my ($sex,$floor)=@_;

	my $birthdays={};
	
	my $items={};

	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>=$floor)&&($record->{sex}=~/$sex/)&&($record->{age} ne ''))
		{
			my $item="$record->{name} \t$record->{country}\t$record->{sex}\t$record->{birthday}\t$record->{rating}\t$record->{age}\t$record->{title}";
			
			$items->{$item}=$record->{rating};
		}
	});
	
	my @items=keys %{$items};
	
	@items=sort
	{
	
		$items->{$b}<=>$items->{$a};
	
	}
	@items;
	
	my $high_rated_txt='';
	
	for(my $i=0;$i<@items;$i++)
	{
	
		$high_rated_txt.=(($i+1).".\t$items[$i]\n");
	
	}
	
	print $high_rated_txt;
	
	$sex=~s/\|//;
	
	save("high_rated_$sex"."_"."above_$floor",$high_rated_txt,"rank\tname\tcountry\tgender\tbirthday\trating\tage\ttitle");

}

sub gm_average_ratings
{

	my $gm_avgr={};

	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>0)&&($record->{sex}=~/F|M/)
		&&($record->{age}>=20)&&($record->{age}<=40)
		&&($record->{title} eq 'GM'))
		{
			$gm_avgr->{"CR$record->{sex}"}+=$record->{rating};
			$gm_avgr->{"R$record->{sex}"}++;
		}
	});
	
	my $RM=$gm_avgr->{RM};
	my $GMAVGRM=averagef($gm_avgr->{CRM},$RM);
	my $RF=$gm_avgr->{RF};
	my $GMAVGRF=averagef($gm_avgr->{CRF},$RF);
	
	my $gm_average_ratings_txt="$RM\t$GMAVGRM\t$RF\t$GMAVGRF\n";
	
	print $gm_average_ratings_txt;
	
	save("gm_average_ratings",$gm_average_ratings_txt,"male GMs\tmale GM average rating\tfemale GMs\tfemale GM average rating");

}

sub iterate_filtered
{

	my $name=shift;
	
	my $term=$terms->{$name};
	
	mkdir "filtered";
	
	my $fcnt=0;
	
	open(FILTERED,">filtered/$name.txt");

	open(PLAYERS,"players.txt");
	
	my $head=<PLAYERS>;
	
	chomp($head);
	
	print FILTERED "cnt\t$head\n";
	
	@listed_fields=split /\t/,$head;
	
	print "\nlisted fields in players.txt:\n\n",join("\t",@listed_fields),"\n\n";
	
	while(my $line=<PLAYERS>)
	{
	
		chomp($line);
		
		my $record={};
		
		my @fields=split /\t/,$line;
		
		foreach(@listed_fields)
		{
		
			my $key=$_;
			
			my $value=shift @fields;
			
			$record->{$key}=$value;
		
		}
		
		if($record->{birthday} ne '')
		{
			$record->{age}=$reference_year-$record->{birthday};
		}
		else
		{
			$record->{age}='';
		}
		
		my $sex=$record->{sex};
		my $rating=$record->{rating};
		my $flag=$record->{flag};
		
		if(eval ($term))
		{
			print FILTERED "$fcnt\t$line\n";
			
			$fcnt++;
			
			if(($fcnt%100)==0)
			{
				print "$fcnt players found\n";
			}
		}
	
	}
	
	close(FILTERED);
	
	close(PLAYERS);

}

sub iterate
{

	my $sub=shift;

	open(PLAYERS,$current_term eq ''?"players.txt":"filtered/$current_term.txt");
	
	my $head=<PLAYERS>;
	
	chomp($head);
	
	@listed_fields=split /\t/,$head;
	
	print "\nlisted fields in players.txt:\n\n",join("\t",@listed_fields),"\n\n";
	
	while(my $line=<PLAYERS>)
	{
	
		chomp($line);
		
		my $record={};
		
		my @fields=split /\t/,$line;
		
		foreach(@listed_fields)
		{
		
			my $key=$_;
			
			my $value=shift @fields;
			
			$record->{$key}=$value;
		
		}
		
		$record->{line}=$line;
		
		if($record->{birthday} ne '')
		{
			$record->{age}=$reference_year-$record->{birthday};
		}
		else
		{
			$record->{age}='';
		}
		
		&{$sub}($record);
	
	}
	
	close(PLAYERS);

}

sub get_key_counts
{

	my ($fc)=@_;

	my $kc='';
	foreach(sort keys(%{$fc}))
	{
	
		my $key=$_;
		
		$kc.="$key\t$fc->{$key}\n";
		
	}
	
	return $kc;

}

sub preprocess
{

	my $cnt=0;

	my ($phase)=@_;

	print "preprocessing players, phase: $phase\n\n";
	
	open(PLAYERS,"players_list_xml.xml");
	
	if($phase eq 'create list')
	{
		
		open(PLAYERS_TXT,">players.txt");
		
		print PLAYERS_TXT join("\t",@listed_fields)."\n";
	
	}
	
	my $chunk='';
	
	my $chars=0;
	
	my $field_counts={};
	my $key_counts={};
	
	while((my $line=<PLAYERS>)&&($cnt<$MAX_CNT))
	{
	
		#print $line;
	
		$chars+=length($line);
	
		chomp($line);
		
		$chunk.=$line;
		
		my @chunk=split /<player>/,$chunk;
		
		if(@chunk>1)
		{
		
			for(my $i=1;$i<@chunk;$i++)
			{
				
				if($chunk[$i]=~/<\/player>/)
				{
				
					my @record=split /<\/player>/,$chunk[$i];
					
					my $record=$record[0];
					
					$chunk=~s/<player>\Q$record<\/player>//;
					
					my $fields={};
					
					while($record=~/<([^>]+)>([^<]+)<\/[^>]+>/g)
					{
						
						my $key=$1;
						my $value=$2;
						
						$fields->{$key}=$value;
						
					}
					
					if($phase eq 'count keys')
					{
					
						my @keys=keys(%{$fields});
						
						@keys=sort(@keys);
						
						my $key_list=join(',',@keys);
						
						$field_counts->{$key_list}++;
						
						map { $key_counts->{$_}++; } @keys;
					
					}
					
					if($phase eq 'create list')
					{
					
						my @fields=();
						foreach(@listed_fields)
						{
						
							my $key=$_;
							
							my $value=$fields->{$key};
							
							push(@fields,$value);
							
						}
						
						my $line_out=join("\t",@fields);
						
						print PLAYERS_TXT "$line_out\n";
					
					}
				
					$cnt++;
					
					if(($cnt%10000)==0)
					{
					
						print "$cnt players processed\n\n";
						
						if($phase eq 'count keys')
						{
						
							my $key_counts=get_key_counts($key_counts);
							
							print "$key_counts\n";
						
						}
					
					}
				
				}
			
			}
			
		}
	
	}
	
	if($phase eq 'create list')
	{
	
		close(PLAYERS_TXT);
	
	}
	
	close(PLAYERS);
	
	if($phase eq 'count keys')
	{
	
		save("key_counts","cnt\t$cnt\n".get_key_counts($key_counts),"key\tcount\t");
		
		save("field_counts",get_key_counts($field_counts),"field\tcount\t");
		
		@listed_fields=get_listed_fields();
	
	}
	
	print "preprocessing OK, $cnt players processed\n";

}