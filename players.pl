use strict;

my $MAX_CNT=1000000;
my $cnt=0;

my @listed_fields=qw(name country sex birthday rating);

my $command='';

while(!($command=~/x/i))
{
	print "\nx = exit\np = preprocess\nh = high rated\n\nenter command: ";
	$command=<>;
	chomp($command);
	
	print "\n";
	
	if($command=~/p/i)
	{
		preprocess();
	}
	
	if($command=~/h/i)
	{
		high_rated();
	}
}

sub high_rated
{

	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>2400)&&($record->{sex} eq 'F'))
		{
			print "$record->{line}\n";
		}
	});

}

sub iterate
{

	my $sub=shift;

	open(PLAYERS,"players.txt");
	
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
		
		&{$sub}($record);
	
	}
	
	close(PLAYERS);

}

sub preprocess
{

	print "preprocessing players\n\n";

	open(PLAYERS,"players_list_xml.xml");
	
	open(PLAYERS_TXT,">players.txt");
	
	my $chunk='';
	
	my $chars=0;
	
	my $field_counts={};
	
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
					
					my @fields=();
					foreach(@listed_fields)
					{
					
						my $key=$_;
						
						my $value=$fields->{$key};
						
						push(@fields,$value);
						
						if($value ne '')
						{
							$field_counts->{$key}++;
						}
						
					}
					
					my $line_out=join("\t",@fields);
					
					print PLAYERS_TXT "$line_out\n";
				
					$cnt++;
					
					if(($cnt%10000)==0)
					{
					
						print "$cnt players processed\n\n";
						
						my @field_counts=();
						foreach(@listed_fields)
						{
						
							my $key=$_;
							
							print "has $key : $field_counts->{$key}\n";
							
						}
						
						print "\n";
					
					}
				
				}
			
			}
			
		}
	
	}
	
	close(PLAYERS_TXT);
	
	close(PLAYERS);
	
	print "preprocessing OK, $cnt players processed\n";

}